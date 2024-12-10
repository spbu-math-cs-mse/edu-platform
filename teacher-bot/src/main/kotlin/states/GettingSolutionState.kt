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
import dev.inmo.tgbotapi.bot.exceptions.CommonRequestException
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.send.media.sendDocument
import dev.inmo.tgbotapi.extensions.api.send.media.sendMediaGroup
import dev.inmo.tgbotapi.extensions.api.send.media.sendPhoto
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import dev.inmo.tgbotapi.requests.abstracts.MultipartFile
import dev.inmo.tgbotapi.requests.abstracts.asMultipartFile
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.media.TelegramMediaPhoto
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.RiskFeature
import io.ktor.server.util.url
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
    } else {
      val getSolution: ContentMessage<*>
      var getMarkup: ContentMessage<*>? = null
      when (solution.type) {
        SolutionType.TEXT ->
          getSolution =
            bot.send(
              state.context,
              solution.content.text!! + "\n\n\n" + solutionInfo(solution),
              replyMarkup = Keyboards.solutionMenu(),
            )

        SolutionType.PHOTO ->
          getSolution =
            bot.sendPhoto(
              state.context,
              getSolutionWithURL(solution.content.filesURL!!.first()),
              text =
              if (solution.content.text == null) {
                solutionInfo(solution)
              } else {
                solution.content.text + "\n\n\n" +
                  solutionInfo(
                    solution,
                  )
              },
              replyMarkup = Keyboards.solutionMenu(),
            )

        SolutionType.DOCUMENT ->
          getSolution =
            bot.sendDocument(
              state.context,
              getSolutionWithURL(solution.content.filesURL!!.first()),
              text =
              if (solution.content.text == null) {
                solutionInfo(solution)
              } else {
                solution.content.text + "\n\n\n" +
                  solutionInfo(
                    solution,
                  )
              },
              replyMarkup = Keyboards.solutionMenu(),
            )

        SolutionType.GROUP -> {
          getSolution =
            bot.sendMediaGroup(
              state.context,
              solution.content.filesURL!!.map {
                TelegramMediaPhoto(
                  getSolutionWithURL(it),
                )
              },
            )
          getMarkup = bot.send(state.context, solutionInfo(solution), replyMarkup = Keyboards.solutionMenu())
        }
      }

      when (val response = flowOf(waitDataCallbackQueryWithUser(state.context.id), waitTextMessageWithUser(state.context.id)).flattenMerge().first()) {
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

            returnBack -> {
              delete(getSolution)
            }
          }
        }
      }
      if (getMarkup != null) {
        delete(getMarkup)
      }
    }
    MenuState(state.context, state.teacherId)
  }
}

object SolutionProvider {
  private var fileIndex = 0

  fun getSolutionWithURL(fileURL: String): MultipartFile {
    println(fileURL)
    val url: URL = URI(fileURL).toURL()
    val outputFileName: String = "solution_${fileIndex++}.pdf"
    val file = File(outputFileName)

    url.openStream().use {
      Channels.newChannel(it).use { rbc ->
        FileOutputStream(outputFileName).use { fos ->
          fos.channel.transferFrom(rbc, 0, Long.MAX_VALUE)
        }
      }
    }

    return file.asMultipartFile()
  }
}
