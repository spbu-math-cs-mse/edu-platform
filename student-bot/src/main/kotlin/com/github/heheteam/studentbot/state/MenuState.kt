package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.errors.toTelegramError
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.state.BotStateWithHandlersAndStudentId
import com.github.heheteam.commonlib.util.HandlerResultWithUserInputOrUnhandled
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.UpdateHandlersController
import com.github.heheteam.commonlib.util.UserInput
import com.github.heheteam.commonlib.util.delete
import com.github.heheteam.commonlib.util.ok
import com.github.heheteam.studentbot.state.quiz.L0Student
import com.github.heheteam.studentbot.state.quiz.QuestState
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.get
import com.github.michaelbull.result.runCatching
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery

class MenuState(override val context: User, override val userId: StudentId) :
  BotStateWithHandlersAndStudentId<State, Unit, StudentApi> {
  private val sentMessages = mutableListOf<AccessibleMessage>()

  override fun defaultState(): State = MenuState(context, userId)

  override suspend fun intro(
    bot: BehaviourContext,
    service: StudentApi,
    updateHandlersController: UpdateHandlersController<() -> Unit, State, FrontendError>,
  ): Result<Unit, FrontendError> = coroutineBinding {
    val stickerMessage = bot.sendSticker(context.id, StudentDialogues.typingSticker)
    val initialMessage =
      bot.send(context, text = StudentDialogues.menu, replyMarkup = StudentKeyboards.menu())
    sentMessages.add(stickerMessage)
    sentMessages.add(initialMessage)
    updateHandlersController.addDataCallbackHandler { it ->
      processKeyboardButtonPresses(it, service)
    }
  }

  private fun processKeyboardButtonPresses(
    callback: DataCallbackQuery,
    service: StudentApi,
  ): HandlerResultWithUserInputOrUnhandled<Nothing, State, Nothing> {
    val state =
      when (callback.data) {
        StudentKeyboards.ABOUT_COURSE -> StudentAboutCourseState(context, userId)
        StudentKeyboards.FREE_ACTIVITY -> {
          val stateName = service.resolveCurrentQuestState(userId).get()
          val state =
            QuestState.restoreState<StudentApi, StudentId>(stateName, context, userId).get()
          state ?: L0Student(context, userId)
        }
        else -> null
      }
    return if (state != null) {
      UserInput(state)
    } else {
      Unhandled
    }
  }

  override suspend fun computeNewState(
    service: StudentApi,
    input: State,
  ): Result<Pair<State, Unit>, FrontendError> {
    return Pair(input, Unit).ok()
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: StudentApi,
    response: Unit,
  ): Result<Unit, FrontendError> =
    runCatching { sentMessages.forEach { message -> bot.delete(message) } }.toTelegramError()

  override suspend fun outro(bot: BehaviourContext, service: StudentApi) = Unit
}
