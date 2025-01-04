package com.github.heheteam.teacherbot.states

import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.SolutionType
import com.github.heheteam.commonlib.util.waitDataCallbackQueryWithUser
import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import com.github.heheteam.teacherbot.Dialogues.noSolutionsToCheck
import com.github.heheteam.teacherbot.Dialogues.solutionInfo
import com.github.heheteam.teacherbot.Keyboards
import com.github.heheteam.teacherbot.Keyboards.returnBack
import com.github.heheteam.teacherbot.TeacherCore
import com.github.heheteam.teacherbot.states.SolutionProvider.getSolutionWithURL
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.send.media.sendDocument
import dev.inmo.tgbotapi.extensions.api.send.media.sendMediaGroup
import dev.inmo.tgbotapi.extensions.api.send.media.sendPhoto
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import dev.inmo.tgbotapi.requests.abstracts.MultipartFile
import dev.inmo.tgbotapi.requests.abstracts.asMultipartFile
import dev.inmo.tgbotapi.types.media.TelegramMediaDocument
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.RiskFeature
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOf
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.net.URL
import java.nio.channels.Channels

@OptIn(RiskFeature::class, ExperimentalCoroutinesApi::class)
fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnGettingSolutionState(
  core: TeacherCore,
) {
  strictlyOn<GettingSolutionState> { state ->

    val teacherId = state.teacherId
    val solution = core.querySolution(teacherId)
    if (solution == null) {
      val reply = bot.send(
        state.context,
        noSolutionsToCheck(),
        replyMarkup = returnBack(),
      )
      waitDataCallbackQuery().first()
      deleteMessage(reply)
      return@strictlyOn MenuState(state.context, state.teacherId)
    }

    val student = core.resolveStudent(solution.studentId)
    if (student.isErr) {
      bot.send(
        state.context,
        student.error.toString(),
      )
      return@strictlyOn MenuState(state.context, state.teacherId)
    }

    val problem = core.resolveProblem(solution.problemId)
    if (problem.isErr) {
      bot.send(
        state.context,
        problem.error.toString(),
      )
      return@strictlyOn MenuState(state.context, state.teacherId)
    }

    val assignment = core.resolveAssignment(problem.value.assignmentId)
    if (assignment.isErr) {
      bot.send(
        state.context,
        assignment.error.toString(),
      )
      return@strictlyOn MenuState(state.context, state.teacherId)
    }

    val getSolution: ContentMessage<*>
    var getMarkup: ContentMessage<*>? = null
    val files: MutableList<Pair<MultipartFile, File>> = mutableListOf()
    when (solution.content.type!!) {
      SolutionType.TEXT ->
        getSolution =
          bot.send(
            state.context,
            solution.content.text!! + "\n\n\n" + solutionInfo(student.value, assignment.value, problem.value),
            replyMarkup = Keyboards.solutionMenu(),
          )

      SolutionType.PHOTO -> {
        files.add(getSolutionWithURL(solution.content.filesURL!!.first()))
        getSolution =
          bot.sendPhoto(
            state.context,
            files.first().first,
            text =
            if (solution.content.text == null) {
              solutionInfo(student.value, assignment.value, problem.value)
            } else {
              solution.content.text + "\n\n\n" +
                solutionInfo(student.value, assignment.value, problem.value)
            },
            replyMarkup = Keyboards.solutionMenu(),
          )
      }

      SolutionType.DOCUMENT -> {
        files.add(getSolutionWithURL(solution.content.filesURL!!.first()))
        getSolution =
          bot.sendDocument(
            state.context,
            files.first().first,
            text =
            if (solution.content.text == null) {
              solutionInfo(student.value, assignment.value, problem.value)
            } else {
              solution.content.text + "\n\n\n" +
                solutionInfo(student.value, assignment.value, problem.value)
            },
            replyMarkup = Keyboards.solutionMenu(),
          )
      }

      SolutionType.GROUP -> {
        files.addAll(
          solution.content.filesURL!!.map {
            getSolutionWithURL(it)
          },
        )
        getSolution =
          bot.sendMediaGroup(
            state.context,
            files.map { TelegramMediaDocument(it.first) },
          )
        getMarkup = bot.send(
          state.context,
          solutionInfo(student.value, assignment.value, problem.value),
          replyMarkup = Keyboards.solutionMenu(),
        )
      }
    }

    when (
      val response =
        flowOf(
          waitDataCallbackQueryWithUser(state.context.id),
          waitTextMessageWithUser(state.context.id)
        ).flattenMerge()
          .first()
    ) {
      is DataCallbackQuery -> {
        val command = response.data
        when (command) {
          Keyboards.goodSolution -> {
            deleteMessage(getSolution)
            // TODO extract from maxscore of a problem
            core.assessSolution(
              solution,
              teacherId,
              SolutionAssessment(1, ""),
            )
          }

          Keyboards.badSolution -> {
            deleteMessage(getSolution)
            core.assessSolution(
              solution,
              teacherId,
              SolutionAssessment(0, ""),
            )
          }

          returnBack -> {
            delete(getSolution)
          }
        }
      }
    }
    if (getMarkup != null) {
      delete(getMarkup)
    }
    files.forEach {
      if (it.second.exists()) {
        it.second.delete()
      }
    }
    MenuState(state.context, state.teacherId)
  }
}

object SolutionProvider {
  private var fileIndex = 0

  fun getSolutionWithURL(fileURL: String): Pair<MultipartFile, File> {
    val url: URL = URI(fileURL).toURL()
    val outputFileName = "solution${fileIndex++}.${fileURL.substringAfterLast(".")}"
    val file = File(outputFileName)

    url.openStream().use {
      Channels.newChannel(it).use { rbc ->
        FileOutputStream(outputFileName).use { fos ->
          fos.channel.transferFrom(rbc, 0, Long.MAX_VALUE)
        }
      }
    }

    return file.asMultipartFile() to file
  }
}
