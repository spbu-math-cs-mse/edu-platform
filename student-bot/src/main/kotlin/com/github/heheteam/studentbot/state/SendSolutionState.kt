package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.SolutionInputRequest
import com.github.heheteam.commonlib.TelegramMessageInfo
import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.state.BotStateWithHandlers
import com.github.heheteam.commonlib.util.HandlerResultWithUserInput
import com.github.heheteam.commonlib.util.HandlingError
import com.github.heheteam.commonlib.util.NewState
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

data class SendSolutionState(
  override val context: User,
  val studentId: StudentId,
  val problem: Problem,
) : BotStateWithHandlers<SolutionInputRequest?, SolutionInputRequest?, StudentApi> {
  lateinit var studentBotToken: String

  override suspend fun intro(
    bot: BehaviourContext,
    service: StudentApi,
    updateHandlersController: UpdateHandlersController<() -> Unit, SolutionInputRequest?, Any>,
  ) {
    bot.send(context, Dialogues.tellValidSolutionTypes(), replyMarkup = back())
    updateHandlersController.addTextMessageHandler { message ->
      if (message.content.text == "/menu") {
        NewState(MenuState(context, studentId))
      } else {
        Unhandled
      }
    }
    updateHandlersController.addTextMessageHandler { message ->
      bot.parseSentSolution(message, studentBotToken)
    }
    updateHandlersController.addMediaMessageHandler { message ->
      bot.setMessageReaction(message, "\uD83E\uDD23")
      bot.parseSentSolution(message, studentBotToken)
    }
    updateHandlersController.addDocumentMessageHandler { message ->
      bot.parseSentSolution(message, studentBotToken)
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
    input: SolutionInputRequest?,
  ): Pair<State, SolutionInputRequest?> {
    return if (input == null) {
      MenuState(context, studentId) to input
    } else {
      ConfirmSubmissionState(context, input) to input
    }
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: StudentApi,
    response: SolutionInputRequest?,
  ) = Unit

  private suspend fun BehaviourContext.parseSentSolution(
    solutionMessage: CommonMessage<*>,
    studentBotToken: String,
  ): HandlerResultWithUserInput<Nothing, SolutionInputRequest, String> {
    val attachment = extractTextWithMediaAttachments(solutionMessage, studentBotToken, this)
    return if (attachment == null) {
      HandlingError(Dialogues.tellSolutionTypeIsInvalid())
    } else {
      UserInput(
        SolutionInputRequest(
          studentId,
          problem.id,
          attachment,
          TelegramMessageInfo(solutionMessage.chat.id.chatId, solutionMessage.messageId),
          LocalDateTime.now().toKotlinLocalDateTime(),
        )
      )
    }
  }

  override suspend fun outro(bot: BehaviourContext, service: StudentApi) = Unit
}
