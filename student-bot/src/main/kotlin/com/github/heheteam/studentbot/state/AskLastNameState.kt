package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.EduPlatformError
import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.state.BotStateWithHandlers
import com.github.heheteam.commonlib.state.UpdateHandlerManager
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.studentbot.Dialogues
import com.github.heheteam.studentbot.Keyboards
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.mapBoth
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User

class AskLastNameState(
  override val context: User,
  private val firstName: String,
  private val token: String?,
) : BotStateWithHandlers<StudentId, Unit, StudentApi> {
  override fun computeNewState(service: StudentApi, input: StudentId): Pair<State, Unit> {
    return MenuState(context, input) to Unit
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: StudentApi,
    response: Unit,
    input: StudentId,
  ) = Unit

  override fun defaultState(): State {
    return StartState(context, token)
  }

  override suspend fun outro(bot: BehaviourContext, service: StudentApi) = Unit

  override suspend fun intro(
    bot: BehaviourContext,
    service: StudentApi,
    updateHandlersController: UpdateHandlerManager<StudentId>,
  ): Result<Unit, EduPlatformError> = coroutineBinding {
    bot.send(context, Dialogues.askLastName(firstName), replyMarkup = Keyboards.back())
    updateHandlersController.addTextMessageHandler { message ->
      val lastName = message.content.text
      val studentId = service.createStudent(firstName, lastName, context.id.chatId.long)
      bot.send(context, Dialogues.niceToMeetYou(firstName, lastName))
      if (token != null) {
        service
          .registerForCourseWithToken(token, studentId)
          .mapBoth(
            success = { course ->
              bot.send(context, Dialogues.successfullyRegisteredForCourse(course, token))
            },
            failure = { error -> bot.send(context, Dialogues.failedToRegisterForCourse(error)) },
          )
      }
      NewState(MenuState(context, studentId))
    }
    updateHandlersController.addDataCallbackHandler { callback ->
      if (callback.data == Keyboards.RETURN_BACK) {
        NewState(AskFirstNameState(context, token))
      } else {
        Unhandled
      }
    }
  }
}
