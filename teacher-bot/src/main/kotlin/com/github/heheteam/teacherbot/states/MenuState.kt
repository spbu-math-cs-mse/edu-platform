package com.github.heheteam.teacherbot.states

import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.api.TeacherStorage
import com.github.heheteam.commonlib.api.toTeacherId
import com.github.heheteam.commonlib.util.waitDataCallbackQueryWithUser
import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import com.github.heheteam.teacherbot.Dialogues
import com.github.heheteam.teacherbot.Keyboards
import com.github.heheteam.teacherbot.logic.SolutionGrader
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.get
import com.github.michaelbull.result.mapBoth
import com.github.michaelbull.result.mapError
import dev.inmo.kslog.common.error
import dev.inmo.kslog.common.logger
import dev.inmo.micro_utils.coroutines.firstNotNull
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import java.time.LocalDateTime
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

class MenuState(override val context: User, val teacherId: TeacherId) : State {
  private val messages = mutableListOf<ContentMessage<*>>()

  suspend fun handle(
    bot: BehaviourContext,
    teacherStorage: TeacherStorage,
    solutionGrader: SolutionGrader,
  ): State =
    with(bot) {
      val (state, response) = readUserInput(this, teacherStorage, solutionGrader)
      sendResponse(bot, response)
      state
    }

  suspend fun readUserInput(
    bot: BehaviourContext,
    service: TeacherStorage,
    solutionGrader: SolutionGrader,
  ): Pair<State, String?> {
    service.updateTgId(teacherId, context.id)
    if (context.username == null) {
      return Pair(StartState(context), null)
    }
    val stickerMessage = bot.sendSticker(context, Dialogues.typingSticker)
    val menuMessage = bot.send(context, Dialogues.menu(), replyMarkup = Keyboards.menu())
    messages.add(stickerMessage)
    messages.add(menuMessage)

    val callbacksFlow =
      bot.waitDataCallbackQueryWithUser(context.id).map { callback ->
        val tryGrading = tryProcessGradingByButtonPress(callback, solutionGrader, teacherId).get()
        if (tryGrading == null) {
          Pair(handleDataCallbackFromMenuButtons(callback.data), null)
          handleDataCallbackFromMenuButtons(callback.data)?.let { Pair(it, null) }
        } else {
          null
        }
      }
    val messagesFlow =
      bot.waitTextMessageWithUser(context.id).map { message ->
        val maybeAssessed = tryParseGradingReply(message, solutionGrader)
        maybeAssessed.mapBoth(
          success = { Pair(MenuState(context, teacherId), "Решение успешно проверено") },
          failure = {
            when (it) {
              is BadAssessment -> Pair(MenuState(context, teacherId), it.error)
              NotReply -> handleCommands(message.content.text)
              ReplyNotToSolution ->
                Pair(
                  MenuState(context, teacherId),
                  "If you want to grade an error, you have to reply to a message below the actual solution",
                )
            }
          },
        )
      }
    return merge(callbacksFlow, messagesFlow).firstNotNull()
  }

  suspend fun sendResponse(bot: TelegramBot, response: String?) {
    messages.forEach { bot.deleteMessage(context, it.messageId) }
    if (response != null) {
      bot.send(context.id, response)
    }
  }

  private fun handleCommands(message: String): Pair<State, String?> {
    val re = Regex("/setid ([0-9]+)")
    val match = re.matchEntire(message)
    return if (match != null) {
      val newId =
        match.groups[1]?.value?.toLongOrNull()
          ?: run {
            logger.error("input id ${match.groups[1]} is not long!")
            return Pair(MenuState(context, teacherId), null)
          }
      Pair(PresetTeacherState(context, newId.toTeacherId()), null)
    } else {
      Pair(MenuState(context, teacherId), "Unrecognized command")
    }
  }

  private fun handleDataCallbackFromMenuButtons(callback: String): State? =
    when (callback) {
      Keyboards.checkGrades -> CheckGradesState(context, teacherId)
      Keyboards.getSolution -> GettingSolutionState(context, teacherId)
      Keyboards.viewStats -> SendStatisticInfoState(context, teacherId)
      else -> null
    }

  fun tryParseGradingReply(
    commonMessage: CommonMessage<TextContent>,
    solutionGrader: SolutionGrader,
  ): Result<Unit, MessageError> = binding {
    val technicalMessageText = extractReplyText(commonMessage).mapError { NotReply }.bind()
    val oldSolutionGradings =
      parseTechnicalMessageContent(technicalMessageText).mapError { ReplyNotToSolution }.bind()
    val assessment =
      extractAssessmentFromMessage(commonMessage).mapError { BadAssessment(it) }.bind()
    val teacherId = TeacherId(1L)
    solutionGrader.assessSolution(
      oldSolutionGradings.solutionId,
      teacherId,
      assessment,
      LocalDateTime.now(),
    )
  }
}

sealed interface MessageError

data object NotReply : MessageError

data object ReplyNotToSolution : MessageError

data class BadAssessment(val error: String) : MessageError
