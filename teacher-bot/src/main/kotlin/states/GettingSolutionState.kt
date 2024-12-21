package com.github.heheteam.teacherbot.states

import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.SolutionType
import com.github.heheteam.commonlib.util.waitDataCallbackQueryWithUser
import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import com.github.heheteam.teacherbot.Dialogues.noSolutionsToCheck
import com.github.heheteam.teacherbot.Dialogues.solutionInfo
import com.github.heheteam.teacherbot.Keyboards
import com.github.heheteam.teacherbot.TeacherCore
import dev.inmo.tgbotapi.bot.exceptions.CommonRequestException
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.send.media.sendDocument
import dev.inmo.tgbotapi.extensions.api.send.media.sendMediaGroup
import dev.inmo.tgbotapi.extensions.api.send.media.sendPhoto
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.requests.abstracts.InputFile
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.media.TelegramMediaPhoto
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.RiskFeature
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOf

@OptIn(RiskFeature::class, ExperimentalCoroutinesApi::class)
fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnGettingSolutionState(
  core: TeacherCore,
) {
  strictlyOn<GettingSolutionState> { state ->

    val teacherId = state.teacherId
    val solution = core.querySolution(teacherId)
    if (solution == null) {
      bot.send(
        state.context,
        noSolutionsToCheck(),
      )
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
    when (solution.type) {
      SolutionType.TEXT ->
        getSolution =
          bot.send(
            state.context,
            solution.content.text!! + "\n\n\n" + solutionInfo(student.value, assignment.value, problem.value),
            replyMarkup = Keyboards.solutionMenu(),
          )

      SolutionType.PHOTO ->
        getSolution =
          bot.sendPhoto(
            state.context,
            InputFile.fromId(solution.content.fileIds!![0]),
            text =
            if (solution.content.text == null) {
              solutionInfo(student.value, assignment.value, problem.value)
            } else {
              solution.content.text + "\n\n\n" +
                solutionInfo(student.value, assignment.value, problem.value)
            },
            replyMarkup = Keyboards.solutionMenu(),
          )

      SolutionType.PHOTOS -> {
        getSolution =
          bot.sendMediaGroup(
            state.context,
            listOf(
              TelegramMediaPhoto(
                InputFile.fromId(solution.content.fileIds!![0]),
                solution.content.text,
              ),
            ) +
              solution.content.fileIds!!
                .map { TelegramMediaPhoto(InputFile.fromId(it)) }
                .drop(1),
          )
        getMarkup = bot.send(
          state.context,
          solutionInfo(student.value, assignment.value, problem.value),
          replyMarkup = Keyboards.solutionMenu(),
        )
      }

      SolutionType.DOCUMENT ->
        getSolution =
          bot.sendDocument(
            state.context,
            InputFile.fromId(solution.content.fileIds!![0]),
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

    when (
      val response =
        flowOf(waitDataCallbackQueryWithUser(state.context.id), waitTextMessageWithUser(state.context.id)).flattenMerge()
          .first()
    ) {
      is DataCallbackQuery -> {
        val command = response.data
        when (command) {
          Keyboards.goodSolution -> {
            try {
              bot.reply(
                ChatId(solution.chatId),
                solution.messageId,
                "good",
              )
            } catch (e: CommonRequestException) {
            }
            deleteMessage(getSolution)
            // TODO extract from maxscore of a problem
            core.assessSolution(
              solution,
              teacherId,
              SolutionAssessment(1, ""),
            )
          }

          Keyboards.badSolution -> {
            try {
              bot.reply(
                ChatId(solution.chatId),
                solution.messageId,
                "bad",
              )
            } catch (e: CommonRequestException) {
            }
            deleteMessage(getSolution)
            core.assessSolution(
              solution,
              teacherId,
              SolutionAssessment(0, ""),
            )
          }

          Keyboards.returnBack -> {
            delete(getSolution)
            if (getMarkup != null) {
              delete(getMarkup)
            }
          }
        }
      }
    }
    MenuState(state.context, state.teacherId)
  }
}
