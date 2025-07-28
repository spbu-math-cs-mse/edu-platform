package com.github.heheteam.studentbot

import com.github.heheteam.commonlib.api.ParentApi
import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.interfaces.ParentId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.state.InformationState
import com.github.heheteam.commonlib.state.SuspendableBotAction
import com.github.heheteam.commonlib.state.registerState
import com.github.heheteam.commonlib.state.registerStateForBotState
import com.github.heheteam.commonlib.state.registerStateWithStudentId
import com.github.heheteam.commonlib.util.HandlerResultWithUserInputOrUnhandled
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.UpdateHandlerManager
import com.github.heheteam.studentbot.state.ApplyForCoursesState
import com.github.heheteam.studentbot.state.AskParentFirstNameState
import com.github.heheteam.studentbot.state.AskParentLastNameState
import com.github.heheteam.studentbot.state.AskStudentFirstNameState
import com.github.heheteam.studentbot.state.AskStudentLastNameState
import com.github.heheteam.studentbot.state.CheckDeadlinesState
import com.github.heheteam.studentbot.state.ConfirmSubmissionState
import com.github.heheteam.studentbot.state.MenuState
import com.github.heheteam.studentbot.state.ParentStartState
import com.github.heheteam.studentbot.state.PetTheDachshundState
import com.github.heheteam.studentbot.state.QueryAssignmentForCheckingGradesState
import com.github.heheteam.studentbot.state.QueryCourseForCheckingDeadlinesState
import com.github.heheteam.studentbot.state.QueryCourseForCheckingGradesState
import com.github.heheteam.studentbot.state.QueryCourseForSubmissionSendingState
import com.github.heheteam.studentbot.state.QueryProblemForSubmissionSendingState
import com.github.heheteam.studentbot.state.RandomActivityState
import com.github.heheteam.studentbot.state.RescheduleDeadlinesState
import com.github.heheteam.studentbot.state.SelectParentGradeState
import com.github.heheteam.studentbot.state.SelectStudentGradeState
import com.github.heheteam.studentbot.state.SelectStudentParentState
import com.github.heheteam.studentbot.state.SendSubmissionState
import com.github.heheteam.studentbot.state.SolutionsStudentMenuState
import com.github.heheteam.studentbot.state.StudentAboutCourseState
import com.github.heheteam.studentbot.state.StudentAboutTeachersState
import com.github.heheteam.studentbot.state.StudentCourseResultsState
import com.github.heheteam.studentbot.state.StudentMethodologyState
import com.github.heheteam.studentbot.state.StudentStartState
import com.github.heheteam.studentbot.state.parent.ParentMenuState
import com.github.heheteam.studentbot.state.parent.registerParentStates
import com.github.heheteam.studentbot.state.quiz.FirstQuestion
import com.github.heheteam.studentbot.state.quiz.ZeroQuestion
import com.github.heheteam.studentbot.state.quiz.registerParentQuests
import com.github.heheteam.studentbot.state.quiz.registerStudentQuests
import com.github.heheteam.studentbot.state.strictlyOnPresetStudentState
import com.github.heheteam.studentbot.state.student.StudentAboutKamenetskiState
import com.github.heheteam.studentbot.state.student.StudentAboutMaximovState
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.types.chat.User

internal class StateRegister(
  private val studentApi: StudentApi,
  private val parentApi: ParentApi,
  private val bot: DefaultBehaviourContextWithFSM<State>,
) {
  @Suppress("LongMethod") // ok, as it only initializes states
  fun registerStates(botToken: String) {
    with(bot) {
      registerStateWithStudentId<StudentAboutCourseState, StudentApi>(
        studentApi,
        ::initializeHandlers,
      )
      registerStateWithStudentId<StudentAboutTeachersState, StudentApi>(
        studentApi,
        ::initializeHandlers,
      )
      registerStateWithStudentId<StudentCourseResultsState, StudentApi>(
        studentApi,
        ::initializeHandlers,
      )
      registerStateWithStudentId<StudentMethodologyState, StudentApi>(
        studentApi,
        ::initializeHandlers,
      )
      strictlyOn<SelectStudentGradeState> { it.handle(this, studentApi) }
      strictlyOn<ConfirmAndGoToQuestState> { it.handle(this, studentApi) }
      registerStateForBotState<StudentStartState, StudentApi>(studentApi)
      registerStateForBotState<AskStudentFirstNameState, StudentApi>(studentApi)
      registerState<AskStudentLastNameState, StudentApi>(studentApi)
      registerSendSubmissionState(botToken, studentApi)
      strictlyOnPresetStudentState(studentApi)
      registerStateWithStudentId<RescheduleDeadlinesState, StudentApi>(studentApi)
      registerStateForBotState<CheckDeadlinesState, StudentApi>(studentApi)
      registerStateForBotState<PetTheDachshundState, StudentApi>(studentApi)
      registerStateWithStudentId<ApplyForCoursesState, StudentApi>(studentApi, ::initializeHandlers)
      registerStateWithStudentId<RandomActivityState, StudentApi>(studentApi, ::initializeHandlers)
      registerStateWithStudentId<MenuState, StudentApi>(studentApi, ::initializeHandlers)
      registerStateWithStudentId<SolutionsStudentMenuState, StudentApi>(
        studentApi,
        ::initializeHandlers,
      )
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
      registerStateWithStudentId<StudentAboutMaximovState, StudentApi>(
        studentApi,
        ::initializeHandlers,
      )
      registerStateWithStudentId<StudentAboutKamenetskiState, StudentApi>(
        studentApi,
        ::initializeHandlers,
      )
      registerStateWithStudentId<InformationState<StudentApi, StudentId>, StudentApi>(
        studentApi,
        ::initializeHandlers,
      )
      registerStateWithStudentId<CourseMenuState, StudentApi>(studentApi, ::initializeHandlers)
      registerStateWithStudentId<MyCoursesState, StudentApi>(studentApi, ::initializeHandlers)
      registerStateWithStudentId<ZeroQuestion, StudentApi>(studentApi, ::initializeHandlers)
      registerStateWithStudentId<FirstQuestion, StudentApi>(studentApi, ::initializeHandlers)
      registerStudentQuests(studentApi, ::initializeHandlers)
      registerParentQuests(parentApi, ::initializeParentsHandlers)
      strictlyOn<SelectStudentParentState> { it.handle(this, studentApi) }
      strictlyOn<AskParentFirstNameState> { it.handle(this, parentApi) }
      strictlyOn<AskParentLastNameState> { it.handle(this, parentApi) }
      strictlyOn<SelectParentGradeState> { it.handle(this, parentApi) }
      strictlyOn<ParentStartState> { it.handle(this, parentApi) }
      registerParentStates(parentApi, ::initializeParentsHandlers)
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
    handlersController: UpdateHandlerManager<out Any?>,
    context: User,
    studentId: StudentId,
  ) {
    handlersController.addTextMessageHandler { maybeCommandMessage ->
      val text = maybeCommandMessage.content.text
      parseCommand(text, studentId, context)
    }
  }

  private fun initializeParentsHandlers(
    handlersController: UpdateHandlerManager<out Any?>,
    context: User,
    parentId: ParentId,
  ) {
    handlersController.addTextMessageHandler { maybeCommandMessage ->
      if (
        maybeCommandMessage.content.text == "/menu" ||
          maybeCommandMessage.content.text.startsWith("/start")
      ) {
        NewState(ParentMenuState(context, parentId))
      } else {
        Unhandled
      }
    }
  }

  private fun parseCommand(
    text: String,
    studentId: StudentId,
    context: User,
  ): HandlerResultWithUserInputOrUnhandled<SuspendableBotAction, Nothing, FrontendError> =
    if (text.startsWith("/menu") || text.startsWith("/start")) {
      NewState(MenuState(context, studentId))
    } else {
      Unhandled
    }
}
