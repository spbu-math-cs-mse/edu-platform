package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.errors.NumberedError
import com.github.heheteam.commonlib.errors.TelegramBotError
import com.github.heheteam.commonlib.errors.TokenError
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.state.BotStateWithHandlers
import com.github.heheteam.commonlib.state.UpdateHandlerManager
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.ok
import com.github.heheteam.studentbot.Dialogues
import com.github.heheteam.studentbot.Keyboards
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.mapBoth
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User

class AskStudentLastNameState(
  override val context: User,
  private val firstName: String,
  private val token: String?,
) : BotStateWithHandlers<StudentId, Unit, StudentApi> {
  override suspend fun computeNewState(
    service: StudentApi,
    input: StudentId,
  ): Result<Pair<State, Unit>, FrontendError> = (MenuState(context, input) to Unit).ok()

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: StudentApi,
    response: Unit,
    input: StudentId,
  ) = Unit.ok()

  override fun defaultState(): State {
    return StartState(context, token)
  }

  override suspend fun outro(bot: BehaviourContext, service: StudentApi) = Unit

  override suspend fun intro(
    bot: BehaviourContext,
    service: StudentApi,
    updateHandlersController: UpdateHandlerManager<StudentId>,
  ): Result<Unit, FrontendError> = coroutineBinding {
    bot.send(context, Dialogues.askLastName(firstName), replyMarkup = Keyboards.back())
    updateHandlersController.addTextMessageHandler { message ->
      val lastName = message.content.text
      NewState(SelectStudentGradeState(context, firstName, lastName))
    }
    updateHandlersController.addDataCallbackHandler { callBack ->
      if (callBack.data == Keyboards.RETURN_BACK) {
        NewState(SelectStudentParentState(context))
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
            when (error) {
              is TelegramBotError -> {}
              is NumberedError -> {
                val deepError = error.error
                if (deepError is TokenError)
                  bot.send(context, Dialogues.failedToRegisterForCourse(deepError))
                else if (!error.shouldBeIgnored) bot.send(context, error.toMessageText())
              }
            }
          },
        )
    }
  }
}
