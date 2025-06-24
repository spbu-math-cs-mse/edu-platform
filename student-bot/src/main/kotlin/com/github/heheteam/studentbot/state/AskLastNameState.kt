package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.EduPlatformError
import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.TokenError
import com.github.heheteam.commonlib.state.BotStateWithHandlers
import com.github.heheteam.commonlib.state.UpdateHandlerManager
import com.github.heheteam.commonlib.util.HandlingError
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
  override suspend fun computeNewState(service: StudentApi, input: StudentId): Pair<State, Unit> {
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
      val maybeStudentId = service.createStudent(firstName, lastName, context.id.chatId.long)
      maybeStudentId.mapBoth(
        success = { studentId ->
          greetUser(lastName, service, studentId, bot)
          NewState(MenuState(context, studentId))
        },
        failure = { HandlingError(it) },
      )
    }
    updateHandlersController.addDataCallbackHandler { callback ->
      if (callback.data == Keyboards.RETURN_BACK) {
        NewState(AskFirstNameState(context, token))
      } else {
        Unhandled
      }
    }
  }

  private suspend fun greetUser(
    lastName: String,
    service: StudentApi,
    studentId: StudentId,
    bot: BehaviourContext,
  ) {
    bot.send(context, Dialogues.niceToMeetYou(firstName, lastName))
    if (token != null) {
      service
        .registerForCourseWithToken(token, studentId)
        .mapBoth(
          success = { course ->
            bot.send(context, Dialogues.successfullyRegisteredForCourse(course, token))
          },
          failure = { error ->
            if (error is TokenError) bot.send(context, Dialogues.failedToRegisterForCourse(error))
            else bot.send(context, "Ошибка: ${error.shortDescription}")
          },
        )
    }
  }
}
