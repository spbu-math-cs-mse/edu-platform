package com.github.heheteam.studentbot

import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.TokenError
import com.github.heheteam.commonlib.state.registerState
import com.github.heheteam.commonlib.state.registerStateWithStudentId
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.UpdateHandlersController
import com.github.heheteam.studentbot.state.ApplyForCoursesState
import com.github.heheteam.studentbot.state.AskFirstNameState
import com.github.heheteam.studentbot.state.AskLastNameState
import com.github.heheteam.studentbot.state.CheckDeadlinesState
import com.github.heheteam.studentbot.state.ConfirmSubmissionState
import com.github.heheteam.studentbot.state.DeveloperStartState
import com.github.heheteam.studentbot.state.MenuState
import com.github.heheteam.studentbot.state.PetTheDachshundState
import com.github.heheteam.studentbot.state.QueryAssignmentForCheckingGradesState
import com.github.heheteam.studentbot.state.QueryCourseForCheckingDeadlinesState
import com.github.heheteam.studentbot.state.QueryCourseForCheckingGradesState
import com.github.heheteam.studentbot.state.QueryCourseForSolutionSendingState
import com.github.heheteam.studentbot.state.QueryProblemForSolutionSendingState
import com.github.heheteam.studentbot.state.RandomActivityState
import com.github.heheteam.studentbot.state.RescheduleDeadlinesState
import com.github.heheteam.studentbot.state.SendSolutionState
import com.github.heheteam.studentbot.state.StartState
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
  fun registerStates(botToken: String) {
    with(bot) {
      registerState<StartState, StudentApi>(studentApi)
      registerState<AskFirstNameState, StudentApi>(studentApi)
      registerState<AskLastNameState, StudentApi>(studentApi)
      registerState<DeveloperStartState, StudentApi>(studentApi)
      registerSendSolutionState(botToken, studentApi)
      strictlyOnPresetStudentState(studentApi)
      registerState<RescheduleDeadlinesState, StudentApi>(studentApi)
      registerState<CheckDeadlinesState, StudentApi>(studentApi)
      registerState<PetTheDachshundState, StudentApi>(studentApi)
      registerStateWithStudentId<ApplyForCoursesState, StudentApi>(studentApi, ::initializeHandlers)
      registerStateWithStudentId<RandomActivityState, StudentApi>(studentApi, ::initializeHandlers)
      registerStateWithStudentId<MenuState, StudentApi>(studentApi, ::initializeHandlers)
      registerStateWithStudentId<ConfirmSubmissionState, StudentApi>(
        studentApi,
        ::initializeHandlers,
      )
      registerStateWithStudentId<QueryCourseForSolutionSendingState, StudentApi>(
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
      registerStateWithStudentId<QueryCourseForSolutionSendingState, StudentApi>(
        studentApi,
        ::initializeHandlers,
      )
      registerStateWithStudentId<QueryProblemForSolutionSendingState, StudentApi>(
        studentApi,
        ::initializeHandlers,
      )
    }
  }

  private fun DefaultBehaviourContextWithFSM<State>.registerSendSolutionState(
    botToken: String,
    studentApi: StudentApi,
  ) {
    strictlyOn<SendSolutionState> { state ->
      state.studentBotToken = botToken
      state.handle(this, studentApi)
    }
  }

  private fun initializeHandlers(
    handlersController: UpdateHandlersController<() -> Unit, out Any?, Any>,
    context: User,
    studentId: StudentId,
  ) {
    handlersController.addTextMessageHandler { maybeCommandMessage ->
      val text = maybeCommandMessage.content.text
      if (text.startsWith("/start")) {
        val parts = text.split(" ")
        if (parts.size != 2) {
          return@addTextMessageHandler NewState(MenuState(context, studentId))
        }
        val token = parts[1].trim()

        studentApi
          .registerForCourseWithToken(token, studentId)
          .mapBoth(
            success = {
              bot.send(context, "Вы успешно записались на курс, используя токен $token")
              NewState(MenuState(context, studentId))
            },
            failure = { error ->
              val errorMessage =
                when (error) {
                  is TokenError.TokenNotFound -> "Такого токена не существует"

                  is TokenError.TokenAlreadyUsed ->
                    "Этот токен уже был использован. <UNK> <UNK> <UNK>."
                }
              bot.send(context, errorMessage)
              NewState(MenuState(context, studentId))
            },
          )
      } else {
        Unhandled
      }
    }

    handlersController.addTextMessageHandler { maybeCommandMessage ->
      if (maybeCommandMessage.content.text == "/menu") {
        NewState(MenuState(context, studentId))
      } else {
        Unhandled
      }
    }
  }
}
