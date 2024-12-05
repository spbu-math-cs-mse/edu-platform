package com.github.heheteam.teacherbot.states

import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.SolutionType
import com.github.heheteam.commonlib.api.TeacherIdRegistry
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
  userIdRegistry: TeacherIdRegistry,
  core: TeacherCore,
) {
  strictlyOn<GettingSolutionState> { state ->
    val userId = userIdRegistry.getUserId(state.context.id).value
    val solution = core.querySolution(userId)
    if (solution == null) {
      bot.send(
        state.context,
        noSolutionsToCheck(),
      )
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
              InputFile.fromId(solution.content.fileIds!![0]),
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
          getMarkup = bot.send(state.context, solutionInfo(solution), replyMarkup = Keyboards.solutionMenu())
        }

        SolutionType.DOCUMENT ->
          getSolution =
            bot.sendDocument(
              state.context,
              InputFile.fromId(solution.content.fileIds!![0]),
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
                userId,
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
                userId,
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
    }
    MenuState(state.context)
  }
}
