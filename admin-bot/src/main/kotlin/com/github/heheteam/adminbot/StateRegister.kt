package com.github.heheteam.adminbot

import com.github.heheteam.adminbot.states.AddAdminState
import com.github.heheteam.adminbot.states.AddStudentState
import com.github.heheteam.adminbot.states.AddTeacherState
import com.github.heheteam.adminbot.states.AskFirstNameState
import com.github.heheteam.adminbot.states.AskLastNameState
import com.github.heheteam.adminbot.states.ConfirmDeleteMessageState
import com.github.heheteam.adminbot.states.CourseInfoState
import com.github.heheteam.adminbot.states.CreateCourseState
import com.github.heheteam.adminbot.states.EditCourseState
import com.github.heheteam.adminbot.states.MenuState
import com.github.heheteam.adminbot.states.PerformDeleteAssignmentState
import com.github.heheteam.adminbot.states.PerformDeleteMessageState
import com.github.heheteam.adminbot.states.QueryAssignmentForDeleting
import com.github.heheteam.adminbot.states.QueryCourseForEditing
import com.github.heheteam.adminbot.states.RemoveStudentState
import com.github.heheteam.adminbot.states.RemoveTeacherState
import com.github.heheteam.adminbot.states.SimpleAdminState
import com.github.heheteam.adminbot.states.StartState
import com.github.heheteam.adminbot.states.assignments.CompleteAssignmentCreationState
import com.github.heheteam.adminbot.states.assignments.CreateAssignmentErrorState
import com.github.heheteam.adminbot.states.assignments.QueryAssignmentDescriptionState
import com.github.heheteam.adminbot.states.assignments.QueryProblemDescriptionsState
import com.github.heheteam.adminbot.states.assignments.QueryStatementsUrlState
import com.github.heheteam.adminbot.states.challenges.CompleteChallengeCreationState
import com.github.heheteam.adminbot.states.challenges.CreateChallengeErrorState
import com.github.heheteam.adminbot.states.challenges.QueryChallengeDescriptionState
import com.github.heheteam.adminbot.states.challenges.QueryChallengeProblemDescriptionsState
import com.github.heheteam.adminbot.states.challenges.QueryChallengeStatementsUrlState
import com.github.heheteam.adminbot.states.general.AdminHandleable
import com.github.heheteam.adminbot.states.scheduled.AddScheduledMessageStartState
import com.github.heheteam.adminbot.states.scheduled.ConfirmScheduledMessageState
import com.github.heheteam.adminbot.states.scheduled.EnterScheduledMessageDateManuallyState
import com.github.heheteam.adminbot.states.scheduled.QueryFullTextConfirmationState
import com.github.heheteam.adminbot.states.scheduled.QueryMessageIdForDeletionState
import com.github.heheteam.adminbot.states.scheduled.QueryNumberOfRecentMessagesState
import com.github.heheteam.adminbot.states.scheduled.QueryScheduledMessageContentState
import com.github.heheteam.adminbot.states.scheduled.QueryScheduledMessageDateState
import com.github.heheteam.adminbot.states.scheduled.QueryScheduledMessageTimeState
import com.github.heheteam.adminbot.states.scheduled.QueryScheduledMessageUserGroupState
import com.github.heheteam.adminbot.states.scheduled.ScheduledMessagesMenuState
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.toAdminId
import com.github.heheteam.commonlib.interfaces.toCourseId
import com.github.heheteam.commonlib.interfaces.toStudentId
import com.github.heheteam.commonlib.state.BotStateWithHandlers
import com.github.heheteam.commonlib.state.SuspendableBotAction
import com.github.heheteam.commonlib.state.registerState
import com.github.heheteam.commonlib.state.registerStateForBotState
import com.github.heheteam.commonlib.util.ActionWrapper
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.UpdateHandlersController
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.error
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.types.chat.User
import kotlinx.datetime.LocalDateTime

@Suppress("TooManyFunctions")
internal class StateRegister(
  private val adminApi: AdminApi,
  private val bot: DefaultBehaviourContextWithFSM<State>,
) {

  private inline fun <
    reified S : BotStateWithHandlers<*, *, AdminApi>
  > DefaultBehaviourContextWithFSM<State>.registerStateForBotStateWithHandlers(
    noinline initUpdateHandlers:
      (
        UpdateHandlersController<SuspendableBotAction, out Any?, FrontendError>, context: User,
      ) -> Unit =
      { _, _ ->
      }
  ) {
    strictlyOn<S> { state -> state.handle(this, adminApi, initUpdateHandlers) }
  }

  fun registerStates(botToken: String) =
    with(bot) {
      onStateOrSubstate<SimpleAdminState> { it.handle(this, adminApi) { _, _ -> } }
      registerStateForBotState<StartState, AdminApi>(adminApi)
      registerStateForBotState<AskFirstNameState, AdminApi>(adminApi)
      registerState<AskLastNameState, AdminApi>(adminApi)
      registerStateForBotStateWithHandlers<CreateCourseState>(::registerHandlers)
      registerScheduledMessagesStates(botToken)
      registerAssignmentCreationStates()
      registerAssignmentDeletionStates()
      registerChallengeCreationStates()
      registerStateForBotStateWithHandlers<AddAdminState>(::registerHandlers)
      registerStateForBotStateWithHandlers<AddStudentState>(::registerHandlers)
      registerStateForBotStateWithHandlers<RemoveStudentState>(::registerHandlers)
      registerStateForBotStateWithHandlers<AddTeacherState>(::registerHandlers)
      registerStateForBotStateWithHandlers<RemoveTeacherState>(::registerHandlers)
      registerStateForBotStateWithHandlers<QueryCourseForEditing>(::registerHandlers)
      registerStateForBotStateWithHandlers<EditCourseState>(::registerHandlers)
      registerStateForBotStateWithHandlers<CourseInfoState>(::registerHandlers)
      registerStateForBotStateWithHandlers<ConfirmDeleteMessageState>(::registerHandlers)
      registerStateForBotStateWithHandlers<PerformDeleteMessageState>(::registerHandlers)
      onStateOrSubstate<AdminHandleable> { it.handleAdmin(this, adminApi, ::registerHandlers) }
    }

  private fun DefaultBehaviourContextWithFSM<State>.registerScheduledMessagesStates(
    botToken: String
  ) {
    registerStateForBotStateWithHandlers<AddScheduledMessageStartState>(::registerHandlers)
    registerStateForBotStateWithHandlers<ConfirmScheduledMessageState>(::registerHandlers)
    registerStateForBotStateWithHandlers<EnterScheduledMessageDateManuallyState>(::registerHandlers)
    registerStateForBotStateWithHandlers<QueryFullTextConfirmationState>(::registerHandlers)
    registerStateForBotStateWithHandlers<QueryMessageIdForDeletionState>(::registerHandlers)
    registerStateForBotStateWithHandlers<QueryNumberOfRecentMessagesState>(::registerHandlers)
    strictlyOn<QueryScheduledMessageContentState> { state ->
      state.adminBotToken = botToken
      state.handle(this, adminApi, ::registerHandlers)
    }
    registerStateForBotStateWithHandlers<QueryScheduledMessageDateState>(::registerHandlers)
    registerStateForBotStateWithHandlers<QueryScheduledMessageTimeState>(::registerHandlers)
    registerStateForBotStateWithHandlers<QueryScheduledMessageUserGroupState>(::registerHandlers)
    registerStateForBotStateWithHandlers<ScheduledMessagesMenuState>(::registerHandlers)
  }

  private fun DefaultBehaviourContextWithFSM<State>.registerAssignmentDeletionStates() {
    registerStateForBotStateWithHandlers<QueryAssignmentForDeleting>(::registerHandlers)
    registerStateForBotState<PerformDeleteAssignmentState, AdminApi>(adminApi)
  }

  private fun DefaultBehaviourContextWithFSM<State>.registerAssignmentCreationStates() {
    registerStateForBotStateWithHandlers<QueryAssignmentDescriptionState>(::registerHandlers)
    registerStateForBotStateWithHandlers<QueryProblemDescriptionsState>(::registerHandlers)
    registerStateForBotStateWithHandlers<QueryStatementsUrlState>(::registerHandlers)
    registerStateForBotStateWithHandlers<CompleteAssignmentCreationState>(::registerHandlers)
    registerStateForBotStateWithHandlers<CreateAssignmentErrorState>(::registerHandlers)
  }

  private fun DefaultBehaviourContextWithFSM<State>.registerChallengeCreationStates() {
    registerStateForBotStateWithHandlers<QueryChallengeDescriptionState>(::registerHandlers)
    registerStateForBotStateWithHandlers<QueryChallengeProblemDescriptionsState>(::registerHandlers)
    registerStateForBotStateWithHandlers<QueryChallengeStatementsUrlState>(::registerHandlers)
    registerStateForBotStateWithHandlers<CompleteChallengeCreationState>(::registerHandlers)
    registerStateForBotStateWithHandlers<CreateChallengeErrorState>(::registerHandlers)
  }

  private fun registerHandlers(
    handlersController: UpdateHandlersController<SuspendableBotAction, out Any?, FrontendError>,
    context: User,
  ) {
    addMenuCommandHandler(handlersController, context)
    addMoveDeadlinesHandler(handlersController, context)
    addGrantAccessToChallengeHandler(handlersController, context)
  }

  private fun addMenuCommandHandler(
    handlersController: UpdateHandlersController<SuspendableBotAction, out Any?, FrontendError>,
    context: User,
  ) {
    handlersController.addTextMessageHandler { maybeCommandMessage ->
      if (maybeCommandMessage.content.text == "/menu") {
        NewState(MenuState(context, DEFAULT_ADMIN_ID.toAdminId()))
      } else {
        Unhandled
      }
    }
  }

  private fun parseAnswerToStudentRequest(
    callback: String,
    action: String,
  ): Pair<StudentId, List<String>> {
    require(callback.startsWith(action))
    val args = callback.drop(action.length).trim().split(' ')
    val studentId = args[0].toLong().toStudentId()
    return studentId to args.drop(1)
  }

  private fun addMoveDeadlinesHandler(
    handlersController: UpdateHandlersController<SuspendableBotAction, out Any?, FrontendError>,
    context: User,
  ) {
    handlersController.addDataCallbackHandler { dataCallbackQuery ->
      if (dataCallbackQuery.data.startsWith(AdminKeyboards.MOVE_DEADLINES)) {
        ActionWrapper {
          val (studentId, args) =
            parseAnswerToStudentRequest(dataCallbackQuery.data, AdminKeyboards.MOVE_DEADLINES)
          val dateTimeString = args.getOrNull(2)
          if (dateTimeString == null) {
            send(context, "Дедлайн ученика с id ${studentId.long} не перенесен")
          } else {
            val newDeadline =
              try {
                LocalDateTime.parse(dateTimeString)
              } catch (e: IllegalArgumentException) {
                KSLog.error("Error parsing date time: ${e.message}")
                return@ActionWrapper
              }
            adminApi.moveAllDeadlinesForStudent(studentId, newDeadline)
            send(context, "Дедлайн ученика с id ${studentId.long} успешно перенесен!")
          }
        }
      } else {
        Unhandled
      }
    }
  }

  private fun addGrantAccessToChallengeHandler(
    handlersController: UpdateHandlersController<SuspendableBotAction, out Any?, FrontendError>,
    context: User,
  ) {
    handlersController.addDataCallbackHandler { dataCallbackQuery ->
      if (dataCallbackQuery.data.startsWith(AdminKeyboards.GRANT_ACCESS_TO_CHALLENGE)) {
        ActionWrapper {
          val (studentId, args) =
            parseAnswerToStudentRequest(
              dataCallbackQuery.data,
              AdminKeyboards.GRANT_ACCESS_TO_CHALLENGE,
            )
          val courseId = args.getOrNull(0)?.toLongOrNull()?.toCourseId()
          if (courseId == null) {
            send(context, "Ученику с id ${studentId.long} отказано в доступе к челленджу")
          } else {
            adminApi.grantAccessToChallengeForStudent(studentId, courseId)
            send(context, "Ученику с id ${studentId.long} успешно предоставлен доступ к челленджу!")
          }
        }
      } else {
        Unhandled
      }
    }
  }
}
