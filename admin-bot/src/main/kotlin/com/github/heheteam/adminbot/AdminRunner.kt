package com.github.heheteam.adminbot

import com.github.heheteam.adminbot.states.AddScheduledMessageStartState
import com.github.heheteam.adminbot.states.AddStudentState
import com.github.heheteam.adminbot.states.AddTeacherState
import com.github.heheteam.adminbot.states.ConfirmDeleteMessageState
import com.github.heheteam.adminbot.states.ConfirmScheduledMessageState
import com.github.heheteam.adminbot.states.CourseInfoState
import com.github.heheteam.adminbot.states.CreateAssignmentErrorState
import com.github.heheteam.adminbot.states.CreateAssignmentState
import com.github.heheteam.adminbot.states.CreateCourseState
import com.github.heheteam.adminbot.states.EditCourseState
import com.github.heheteam.adminbot.states.EnterScheduledMessageDateManuallyState
import com.github.heheteam.adminbot.states.MenuState
import com.github.heheteam.adminbot.states.PerformDeleteMessageState
import com.github.heheteam.adminbot.states.QueryCourseForEditing
import com.github.heheteam.adminbot.states.QueryFullTextConfirmationState
import com.github.heheteam.adminbot.states.QueryMessageIdForDeletionState
import com.github.heheteam.adminbot.states.QueryNumberOfRecentMessagesState
import com.github.heheteam.adminbot.states.QueryScheduledMessageContentState
import com.github.heheteam.adminbot.states.QueryScheduledMessageDateState
import com.github.heheteam.adminbot.states.QueryScheduledMessageTimeState
import com.github.heheteam.adminbot.states.RemoveStudentState
import com.github.heheteam.adminbot.states.RemoveTeacherState
import com.github.heheteam.commonlib.EduPlatformError
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.interfaces.toAdminId
import com.github.heheteam.commonlib.interfaces.toStudentId
import com.github.heheteam.commonlib.state.BotStateWithHandlers
import com.github.heheteam.commonlib.util.ActionWrapper
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.UpdateHandlersController
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.error
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.bot.setMyCommands
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndFSMAndStartLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.command
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import dev.inmo.tgbotapi.types.BotCommand
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.utils.RiskFeature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime

private const val DEFAULT_ADMIN_ID = 0L

class AdminRunner(private val adminApi: AdminApi) {
  private inline fun <
    reified S : BotStateWithHandlers<*, *, AdminApi>
  > DefaultBehaviourContextWithFSM<State>.registerStateForBotStateWithHandlers(
    noinline initUpdateHandlers:
      (
        UpdateHandlersController<BehaviourContext.() -> Unit, out Any?, EduPlatformError>,
        context: User,
      ) -> Unit =
      { _, _ ->
      }
  ) {
    strictlyOn<S> { state -> state.handle(this, adminApi, initUpdateHandlers) }
  }

  @OptIn(RiskFeature::class)
  suspend fun run(botToken: String) {
    telegramBotWithBehaviourAndFSMAndStartLongPolling(
        botToken,
        CoroutineScope(Dispatchers.IO),
        onStateHandlingErrorHandler = { state, e ->
          println("Thrown error in AdminBot on $state")
          e.printStackTrace()
          state
        },
      ) {
        println(getMe())

        setMyCommands(
          listOf(BotCommand("start", "Start bot"), BotCommand("menu", "Resend menu message"))
        )
        command("start") {
          val user = it.from
          if (user != null) startChain(MenuState(user, DEFAULT_ADMIN_ID.toAdminId()))
        }

        registerAllStates()

        allUpdatesFlow.subscribeSafelyWithoutExceptions(this) { println(it) }
      }
      .second
      .join()
  }

  private fun DefaultBehaviourContextWithFSM<State>.registerAllStates() {
    registerStateForBotStateWithHandlers<MenuState>(::registerHandlers)
    registerStateForBotStateWithHandlers<CreateCourseState>(::registerHandlers)
    registerStateForBotStateWithHandlers<CreateAssignmentState>(::registerHandlers)
    registerStateForBotStateWithHandlers<CreateAssignmentErrorState>(::registerHandlers)
    registerStateForBotStateWithHandlers<AddStudentState>(::registerHandlers)
    registerStateForBotStateWithHandlers<RemoveStudentState>(::registerHandlers)
    registerStateForBotStateWithHandlers<AddTeacherState>(::registerHandlers)
    registerStateForBotStateWithHandlers<RemoveTeacherState>(::registerHandlers)
    registerStateForBotStateWithHandlers<QueryCourseForEditing>(::registerHandlers)
    registerStateForBotStateWithHandlers<EditCourseState>(::registerHandlers)
    registerStateForBotStateWithHandlers<CourseInfoState>(::registerHandlers)
    registerStateForBotStateWithHandlers<QueryNumberOfRecentMessagesState>(::registerHandlers)
    registerStateForBotStateWithHandlers<QueryFullTextConfirmationState>(::registerHandlers)
    registerStateForBotStateWithHandlers<QueryMessageIdForDeletionState>(::registerHandlers)
    registerStateForBotStateWithHandlers<ConfirmDeleteMessageState>(::registerHandlers)
    registerStateForBotStateWithHandlers<PerformDeleteMessageState>(::registerHandlers)
    registerStateForBotStateWithHandlers<AddScheduledMessageStartState>(::registerHandlers)
    registerStateForBotStateWithHandlers<QueryScheduledMessageContentState>(::registerHandlers)
    registerStateForBotStateWithHandlers<QueryScheduledMessageDateState>(::registerHandlers)
    registerStateForBotStateWithHandlers<EnterScheduledMessageDateManuallyState>(::registerHandlers)
    registerStateForBotStateWithHandlers<QueryScheduledMessageTimeState>(::registerHandlers)
    registerStateForBotStateWithHandlers<ConfirmScheduledMessageState>(::registerHandlers)
  }

  private fun registerHandlers(
    handlersController:
      UpdateHandlersController<BehaviourContext.() -> Unit, out Any?, EduPlatformError>,
    context: User,
  ) {
    addMenuCommandHandler(handlersController, context)
    addMoveDeadlinesHandler(handlersController, context)
  }

  private fun addMenuCommandHandler(
    handlersController:
      UpdateHandlersController<BehaviourContext.() -> Unit, out Any?, EduPlatformError>,
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

  private fun addMoveDeadlinesHandler(
    handlersController:
      UpdateHandlersController<BehaviourContext.() -> Unit, out Any?, EduPlatformError>,
    context: User,
  ) {
    handlersController.addDataCallbackHandler { dataCallbackQuery ->
      if (dataCallbackQuery.data.startsWith(AdminKeyboards.MOVE_DEADLINES)) {
        ActionWrapper {
          println(dataCallbackQuery.data)
          val args = dataCallbackQuery.data.drop(AdminKeyboards.MOVE_DEADLINES.length).split(' ')
          val studentId = args.getOrNull(1)?.toLongOrNull()?.toStudentId()
          val dateTimeString = args.getOrNull(2)
          if (studentId == null) {
            KSLog.error("Unexpected data callback query \"${dataCallbackQuery.data}\"")
          } else if (dateTimeString == null) {
            runBlocking(Dispatchers.IO) {
              send(context, "Дедлайн ученика с id ${studentId.long} не перенесен")
            }
          } else {
            val newDeadline =
              try {
                LocalDateTime.parse(dateTimeString)
              } catch (e: IllegalArgumentException) {
                KSLog.error("Error parsing date time: ${e.message}")
                return@ActionWrapper
              }
            adminApi.moveAllDeadlinesForStudent(studentId, newDeadline)
            runBlocking(Dispatchers.IO) {
              send(context, "Дедлайн ученика с id ${studentId.long} успешно перенесен!")
            }
          }
        }
      } else {
        Unhandled
      }
    }
  }
}
