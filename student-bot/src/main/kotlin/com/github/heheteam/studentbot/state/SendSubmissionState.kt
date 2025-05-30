package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.SubmissionInputRequest
import com.github.heheteam.commonlib.TelegramMessageInfo
import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.state.BotStateWithHandlersAndStudentId
import com.github.heheteam.commonlib.util.HandlerResultWithUserInput
import com.github.heheteam.commonlib.util.HandlingError
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.UpdateHandlersController
import com.github.heheteam.commonlib.util.UserInput
import com.github.heheteam.commonlib.util.extractTextWithMediaAttachments
import com.github.heheteam.studentbot.Dialogues
import com.github.heheteam.studentbot.Keyboards.RETURN_BACK
import com.github.heheteam.studentbot.metaData.back
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.api.send.setMessageReaction
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import java.time.LocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime

data class SendSubmissionState(
  override val context: User,
  override val userId: StudentId,
  val problem: Problem,
) : BotStateWithHandlersAndStudentId<SubmissionInputRequest?, SubmissionInputRequest?, StudentApi> {
  lateinit var studentBotToken: String

  override suspend fun intro(
    bot: BehaviourContext,
    service: StudentApi,
    updateHandlersController: UpdateHandlersController<() -> Unit, SubmissionInputRequest?, Any>,
  ) {
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

  override fun computeNewState(
    service: StudentApi,
    input: SubmissionInputRequest?,
  ): Pair<State, SubmissionInputRequest?> {
    return if (input == null) {
      MenuState(context, userId) to input
    } else {
      ConfirmSubmissionState(context, userId, input) to input
    }
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: StudentApi,
    response: SubmissionInputRequest?,
  ) = Unit

  private suspend fun BehaviourContext.parseSentSubmission(
    submissionMessage: CommonMessage<*>,
    studentBotToken: String,
  ): HandlerResultWithUserInput<Nothing, SubmissionInputRequest, String> {
    val attachment = extractTextWithMediaAttachments(submissionMessage, studentBotToken, this)
    return if (attachment == null) {
      HandlingError(Dialogues.tellSubmissionTypeIsInvalid)
    } else {
      UserInput(
        SubmissionInputRequest(
          userId,
          problem.id,
          attachment,
          TelegramMessageInfo(submissionMessage.chat.id.chatId, submissionMessage.messageId),
          LocalDateTime.now().toKotlinLocalDateTime(),
        )
      )
    }
  }

  override suspend fun outro(bot: BehaviourContext, service: StudentApi) = Unit
}
