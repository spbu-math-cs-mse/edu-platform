package com.github.heheteam.studentbot

import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.errors.TokenError
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.state.registerState
import com.github.heheteam.commonlib.state.registerStateForBotState
import com.github.heheteam.commonlib.state.registerStateWithStudentId
import com.github.heheteam.commonlib.util.HandlerResultWithUserInputOrUnhandled
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.UpdateHandlersController
import com.github.heheteam.studentbot.state.ApplyForCoursesState
import com.github.heheteam.studentbot.state.AskFirstNameState
import com.github.heheteam.studentbot.state.AskLastNameState
import com.github.heheteam.studentbot.state.CheckDeadlinesState
import com.github.heheteam.studentbot.state.ConfirmSubmissionState
import com.github.heheteam.studentbot.state.MenuState
import com.github.heheteam.studentbot.state.PetTheDachshundState
import com.github.heheteam.studentbot.state.QueryAssignmentForCheckingGradesState
import com.github.heheteam.studentbot.state.QueryCourseForCheckingDeadlinesState
import com.github.heheteam.studentbot.state.QueryCourseForCheckingGradesState
import com.github.heheteam.studentbot.state.QueryCourseForSubmissionSendingState
import com.github.heheteam.studentbot.state.QueryProblemForSubmissionSendingState
import com.github.heheteam.studentbot.state.RandomActivityState
import com.github.heheteam.studentbot.state.RescheduleDeadlinesState
import com.github.heheteam.studentbot.state.SendSubmissionState
import com.github.heheteam.studentbot.state.StartState
import com.github.heheteam.studentbot.state.WhoAmIState
import com.github.heheteam.studentbot.state.strictlyOnPresetStudentState
import com.github.michaelbull.result.mapBoth
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.types.chat.User

internal class StateRegister(
  private val studentApi: StudentApi,
  private val bot: DefaultBehaviourContextWithFSM<State>,
) {
  @Suppress("LongMethod") // ok, as it only initializes states
  fun registerStates(botToken: String) {
    with(bot) {
      registerStateForBotState<StartState, StudentApi>(studentApi)
      registerStateForBotState<AskFirstNameState, StudentApi>(studentApi)
      registerState<AskLastNameState, StudentApi>(studentApi)
      registerSendSubmissionState(botToken, studentApi)
      strictlyOnPresetStudentState(studentApi)
      registerStateWithStudentId<RescheduleDeadlinesState, StudentApi>(studentApi)
      registerStateForBotState<CheckDeadlinesState, StudentApi>(studentApi)
      registerStateForBotState<PetTheDachshundState, StudentApi>(studentApi)
      registerStateWithStudentId<ApplyForCoursesState, StudentApi>(studentApi, ::initializeHandlers)
      registerStateWithStudentId<RandomActivityState, StudentApi>(studentApi, ::initializeHandlers)
      registerStateWithStudentId<MenuState, StudentApi>(studentApi, ::initializeHandlers)
      registerStateForBotState<WhoAmIState, StudentApi>(studentApi)
      registerStateWithStudentId<ConfirmSubmissionState, StudentApi>(
        studentApi,
        ::initializeHandlers,
      )
      registerStateWithStudentId<QueryCourseForSubmissionSendingState, StudentApi>(
        studentApi,
        ::initializeHandlers,
      )
      registerStateWithStudentId<QueryCourseForCheckingGradesState, StudentApi>(
        studentApi,
        ::initializeHandlers,
      )
      registerStateWithStudentId<QueryCourseForCheckingDeadlinesState, StudentApi>(
        studentApi,
        ::initializeHandlers,
      )
      registerStateWithStudentId<QueryAssignmentForCheckingGradesState, StudentApi>(
        studentApi,
        ::initializeHandlers,
      )
      registerStateWithStudentId<QueryCourseForSubmissionSendingState, StudentApi>(
        studentApi,
        ::initializeHandlers,
      )
      registerStateWithStudentId<QueryProblemForSubmissionSendingState, StudentApi>(
        studentApi,
        ::initializeHandlers,
      )
    }
  }

  private fun DefaultBehaviourContextWithFSM<State>.registerSendSubmissionState(
    botToken: String,
    studentApi: StudentApi,
  ) {
    strictlyOn<SendSubmissionState> { state ->
      state.studentBotToken = botToken
      state.handle(this, studentApi)
    }
  }

  private fun initializeHandlers(
    handlersController: UpdateHandlersController<() -> Unit, out Any?, FrontendError>,
    context: User,
    studentId: StudentId,
  ) {
    handlersController.addTextMessageHandler { maybeCommandMessage ->
      val text = maybeCommandMessage.content.text
      parseCommand(text, studentId, context)
    }

    handlersController.addTextMessageHandler { maybeCommandMessage ->
      if (maybeCommandMessage.content.text == "/menu") {
        NewState(MenuState(context, studentId))
      } else {
        Unhandled
      }
    }
  }

  private suspend fun parseCommand(
    text: String,
    studentId: StudentId,
    context: User,
  ): HandlerResultWithUserInputOrUnhandled<() -> Unit, Nothing, FrontendError> =
    if (text.startsWith("/start")) {
      val parts = text.split(" ")
      if (parts.size != 2) {
        NewState(MenuState(context, studentId))
      } else {
        val token = parts[1].trim()
        studentApi
          .registerForCourseWithToken(token, studentId)
          .mapBoth(
            success = { course ->
              bot.send(context, Dialogues.successfullyRegisteredForCourse(course, token))
            },
            failure = { error ->
              val deepError = error.error
              if (deepError is TokenError)
                bot.send(context, Dialogues.failedToRegisterForCourse(deepError))
              else if (!error.shouldBeIgnored) bot.send(context, error.toMessageText())
            },
          )
        NewState(MenuState(context, studentId))
      }
    } else {
      Unhandled
    }
}
