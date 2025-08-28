package com.github.heheteam.adminbot.states

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.state.BotStateWithHandlers
import com.github.heheteam.commonlib.state.UpdateHandlersControllerDefault
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.UserInput
import com.github.heheteam.commonlib.util.createAssignmentPicker
import com.github.heheteam.commonlib.util.delete
import com.github.heheteam.commonlib.util.ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.mapBoth
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.warning
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.bot.exceptions.CommonRequestException
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage

abstract class QueryAssignmentState(
  override val context: User,
  val adminId: AdminId,
  val courseId: CourseId,
) : BotStateWithHandlers<Assignment?, Unit, AdminApi> {
  private val sentMessages = mutableListOf<AccessibleMessage>()

  override fun defaultState(): State = MenuState(context, adminId)

  override suspend fun intro(
    bot: BehaviourContext,
    service: AdminApi,
    updateHandlersController: UpdateHandlersControllerDefault<Assignment?>,
  ): Result<Unit, FrontendError> = coroutineBinding {
    val assignments = service.getAssignments(courseId).bind()
    val assignmentPicker = createAssignmentPicker(assignments)
    val message =
      bot.sendMessage(context.id, "Выберите серию", replyMarkup = assignmentPicker.keyboard)
    sentMessages.add(message)
    updateHandlersController.addTextMessageHandler { maybeCommandMessage ->
      if (maybeCommandMessage.content.text == "/menu") {
        NewState(MenuState(context, adminId))
      } else {
        Unhandled
      }
    }
    updateHandlersController.addDataCallbackHandler { dataCallbackQuery ->
      assignmentPicker
        .handler(dataCallbackQuery.data)
        .mapBoth(success = { UserInput(it) }, failure = { Unhandled })
    }
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: AdminApi,
    response: Unit,
    input: Assignment?,
  ): Result<Unit, FrontendError> = Unit.ok()

  override suspend fun outro(bot: BehaviourContext, service: AdminApi) {
    sentMessages.forEach {
      try {
        bot.delete(it)
      } catch (e: CommonRequestException) {
        KSLog.warning("Failed to delete message", e)
      }
    }
  }
}
