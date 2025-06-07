package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.EduPlatformError
import com.github.heheteam.commonlib.SubmissionInputRequest
import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.logic.SubmissionSendingResult
import com.github.heheteam.commonlib.state.BotStateWithHandlersAndStudentId
import com.github.heheteam.commonlib.toStackedString
import com.github.heheteam.commonlib.util.ButtonData
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.UpdateHandlersController
import com.github.heheteam.commonlib.util.UserInput
import com.github.heheteam.commonlib.util.buildColumnMenu
import com.github.heheteam.commonlib.util.sendTextWithMediaAttachments
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.mapBoth
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.warning
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.bot.exceptions.CommonRequestException
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery

class ConfirmSubmissionState(
  override val context: User,
  override val userId: StudentId,
  private val submissionInputRequest: SubmissionInputRequest,
) :
  BotStateWithHandlersAndStudentId<
    Boolean,
    SubmissionSendingResult?,
    StudentApi,
  > { // null Out implies the user did not confirm the submission
  private lateinit var submissionMessage: AccessibleMessage
  private lateinit var confirmMessage: AccessibleMessage

  override fun defaultState(): State = MenuState(context, userId)

  override suspend fun intro(
    bot: BehaviourContext,
    service: StudentApi,
    updateHandlersController: UpdateHandlersController<() -> Unit, Boolean, Any>,
  ): Result<Unit, EduPlatformError> = coroutineBinding {
    val confirmMessageKeyboard =
      buildColumnMenu(
        ButtonData("Да", "yes") { true },
        ButtonData("Нет (отменить отправку)", "no") { false },
      )
    submissionMessage =
      bot.sendTextWithMediaAttachments(context.id, submissionInputRequest.submissionContent).value
    confirmMessage =
      bot.sendMessage(
        context,
        "Вы уверены, что хотите отправить это решение?",
        replyMarkup = confirmMessageKeyboard.keyboard,
      )
    updateHandlersController.addDataCallbackHandler { value: DataCallbackQuery ->
      val result = confirmMessageKeyboard.handler.invoke(value.data)
      result.mapBoth(success = { UserInput(it) }, failure = { Unhandled })
    }
  }

  override fun computeNewState(
    service: StudentApi,
    input: Boolean,
  ): Pair<State, SubmissionSendingResult?> {
    val menuState = MenuState(context, submissionInputRequest.studentId)
    return menuState to
      if (input) {
        service.inputSubmission(submissionInputRequest)
      } else {
        null
      }
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: StudentApi,
    response: SubmissionSendingResult?,
  ) {
    with(bot) {
      try {
        delete(submissionMessage)
      } catch (e: CommonRequestException) {
        KSLog.warning("Failed to delete message", e)
      }
      try {
        delete(confirmMessage)
      } catch (e: CommonRequestException) {
        KSLog.warning("Failed to delete message", e)
      }
    }
    if (response != null) {
      when (response) {
        is SubmissionSendingResult.Failure ->
          bot.send(
            context.id,
            "Случилась ошибка при отправке решения\n" + response.error.toStackedString(),
          )
        is SubmissionSendingResult.Success ->
          bot.send(
            context.id,
            "Решение ${response.submissionId.long} успешно отправлено на проверку!",
          )
      }
    }
  }

  override suspend fun outro(bot: BehaviourContext, service: StudentApi) = Unit
}
