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
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.accessibleMessageOrNull
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import java.time.LocalDateTime
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

class MenuState(override val context: User, val teacherId: TeacherId) : State {
//  private val messages = mutableListOf<ContentMessage<*>>()

  suspend fun handle(
    bot: BehaviourContext,
    teacherStorage: TeacherStorage,
    solutionGrader: SolutionGrader,
  ): State =
    with(bot) {
      val state = readUserInput(this, teacherStorage, solutionGrader)
      state
    }

  private suspend fun readUserInput(
    bot: BehaviourContext,
    service: TeacherStorage,
    solutionGrader: SolutionGrader,
  ): State {
    service.updateTgId(teacherId, context.id)
    if (context.username == null) {
      return StartState(context)
    }
    bot.send(context, Dialogues.menu(), replyMarkup = Keyboards.menu())

    val callbacksFlow =
      bot.waitDataCallbackQueryWithUser(context.id).map { callback ->
        tryProcessGradingByButtonPress(callback, solutionGrader, teacherId).get()
        null
      }
    val messagesFlow =
      bot.waitTextMessageWithUser(context.id).map { message ->
        val maybeAssessed = tryParseGradingReply(message, solutionGrader)
        maybeAssessed
          .mapBoth(
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
          .run {
            second?.let { it1 -> bot.replyOrSend(message, it1) }
            first
          }
      }
    return merge(callbacksFlow, messagesFlow).firstNotNull()
  }

  private suspend fun BehaviourContext.replyOrSend(message: CommonMessage<*>, text: String) {
    val accessibleMessage = message.replyTo?.accessibleMessageOrNull()
    if (accessibleMessage != null) reply(accessibleMessage, text = text)
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

  private fun tryParseGradingReply(
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
