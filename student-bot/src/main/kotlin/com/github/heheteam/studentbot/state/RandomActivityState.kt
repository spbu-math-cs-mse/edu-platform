package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.errors.NumberedError
import com.github.heheteam.commonlib.errors.toNumberedResult
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.state.BotStateWithHandlersAndStudentId
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.UpdateHandlersController
import com.github.heheteam.commonlib.util.delete
import com.github.heheteam.commonlib.util.ok
import com.github.heheteam.studentbot.Keyboards
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.runCatching
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage

data class RandomActivityState(override val context: User, override val userId: StudentId) :
  BotStateWithHandlersAndStudentId<Unit, Unit, StudentApi> {
  private val sentMessages = mutableListOf<AccessibleMessage>()

  override fun defaultState(): State = MenuState(context, userId)

  override suspend fun intro(
    bot: BehaviourContext,
    service: StudentApi,
    updateHandlersController: UpdateHandlersController<() -> Unit, Unit, NumberedError>,
  ): Result<Unit, NumberedError> = coroutineBinding {
    val selectCourseMessage =
      bot.sendMessage(context.id, "Что-то бесплатное", replyMarkup = Keyboards.back())
    sentMessages.add(selectCourseMessage)
    updateHandlersController.addDataCallbackHandler { dataCallbackQuery ->
      if (dataCallbackQuery.data == Keyboards.RETURN_BACK) {
        NewState(MenuState(context, userId))
      } else {
        Unhandled
      }
    }
  }

  override suspend fun computeNewState(
    service: StudentApi,
    input: Unit,
  ): Result<Pair<State, Unit>, NumberedError> = (MenuState(context, userId) to Unit).ok()

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: StudentApi,
    response: Unit,
  ): Result<Unit, NumberedError> =
    runCatching { sentMessages.forEach { message -> bot.delete(message) } }.toNumberedResult()

  override suspend fun outro(bot: BehaviourContext, service: StudentApi) = Unit
}
