package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.SubmissionInputRequest
import com.github.heheteam.commonlib.TeacherResolveError
import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.SubmissionId
import com.github.heheteam.commonlib.state.BotStateWithHandlersAndStudentId
import com.github.heheteam.commonlib.toStackedString
import com.github.heheteam.commonlib.util.ButtonData
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.UpdateHandlersController
import com.github.heheteam.commonlib.util.UserInput
import com.github.heheteam.commonlib.util.buildColumnMenu
import com.github.heheteam.commonlib.util.sendTextWithMediaAttachments
import com.github.michaelbull.result.Result
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
    Result<SubmissionId, TeacherResolveError>?,
    StudentApi,
  > { // null Out implies the user did not confirm the submission
  private lateinit var submissionMessage: AccessibleMessage
  private lateinit var confirmMessage: AccessibleMessage

  override suspend fun intro(
    bot: BehaviourContext,
    service: StudentApi,
    updateHandlersController: UpdateHandlersController<() -> Unit, Boolean, Any>,
  ) {

    val confirmMessageKeyboard =
      buildColumnMenu(
        ButtonData("Да", "yes") { true },
        ButtonData("Нет (отменить отправку)", "no") { false },
      )
    submissionMessage =
      bot.sendTextWithMediaAttachments(context.id, submissionInputRequest.submissionContent)
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
  ): Pair<State, Result<SubmissionId, TeacherResolveError>?> {
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
    response: Result<SubmissionId, TeacherResolveError>?,
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
    response?.mapBoth(
      success = { bot.send(context.id, "Решение ${it.long} успешно отправлено на проверку!") },
      failure = {
        bot.send(context.id, "Случилась ошибка при отправке решения\n" + it.toStackedString())
      },
    )
  }

  override suspend fun outro(bot: BehaviourContext, service: StudentApi) = Unit
}
