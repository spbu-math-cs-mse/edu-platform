package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.SubmissionInputRequest
import com.github.heheteam.commonlib.TelegramMessageInfo
import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.errors.toTelegramError
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.state.BotStateWithHandlersAndStudentId
import com.github.heheteam.commonlib.util.HandlerResultWithUserInputOrUnhandled
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.UpdateHandlerManager
import com.github.heheteam.commonlib.util.UserInput
import com.github.heheteam.commonlib.util.extractTextWithMediaAttachments
import com.github.heheteam.commonlib.util.getCurrentMoscowTime
import com.github.heheteam.commonlib.util.ok
import com.github.heheteam.studentbot.Dialogues
import com.github.heheteam.studentbot.Keyboards.RETURN_BACK
import com.github.heheteam.studentbot.metaData.back
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.runCatching
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.api.send.setMessageReaction
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage

data class SendSubmissionState(
  override val context: User,
  override val userId: StudentId,
  val problem: Problem,
) : BotStateWithHandlersAndStudentId<SubmissionInputRequest?, SubmissionInputRequest?, StudentApi> {
  lateinit var studentBotToken: String

  override fun defaultState(): State = MenuState(context, userId)

  override suspend fun intro(
    bot: BehaviourContext,
    service: StudentApi,
    updateHandlersController: UpdateHandlerManager<SubmissionInputRequest?>,
  ): Result<Unit, FrontendError> = coroutineBinding {
    bot.send(context, Dialogues.tellValidSubmissionTypes, replyMarkup = back())
    updateHandlersController.addTextMessageHandler { message ->
      bot.parseSentSubmission(message, studentBotToken)
    }
    updateHandlersController.addMediaMessageHandler { message ->
      bot.setMessageReaction(message, "\uD83E\uDD23")
      bot.parseSentSubmission(message, studentBotToken)
    }
    updateHandlersController.addDocumentMessageHandler { message ->
      bot.parseSentSubmission(message, studentBotToken)
    }
    updateHandlersController.addDataCallbackHandler { dataCallbackQuery ->
      if (dataCallbackQuery.data == RETURN_BACK) {
        UserInput(null)
      } else {
        Unhandled
      }
    }
  }

  override suspend fun computeNewState(
    service: StudentApi,
    input: SubmissionInputRequest?,
  ): Result<Pair<State, SubmissionInputRequest?>, FrontendError> {
    return if (input == null) {
        MenuState(context, userId) to input
      } else {
        ConfirmSubmissionState(context, userId, input) to input
      }
      .ok()
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: StudentApi,
    response: SubmissionInputRequest?,
    input: SubmissionInputRequest?,
  ): Result<Unit, FrontendError> =
    runCatching {
        if (response == null) {
          bot.send(context, Dialogues.tellSubmissionTypeIsInvalid)
        }
      }
      .toTelegramError()

  private suspend fun BehaviourContext.parseSentSubmission(
    submissionMessage: CommonMessage<*>,
    studentBotToken: String,
  ): HandlerResultWithUserInputOrUnhandled<Nothing, SubmissionInputRequest?, FrontendError> {
    val attachment = extractTextWithMediaAttachments(submissionMessage, studentBotToken, this)
    return if (attachment == null) {
      UserInput(null)
    } else {
      UserInput(
        SubmissionInputRequest(
          userId,
          problem.id,
          attachment,
          TelegramMessageInfo(submissionMessage.chat.id.chatId, submissionMessage.messageId),
          getCurrentMoscowTime(),
        )
      )
    }
  }

  override suspend fun outro(bot: BehaviourContext, service: StudentApi) = Unit
}
