package com.github.heheteam.adminbot

import com.github.heheteam.adminbot.states.AddAdminState
import com.github.heheteam.adminbot.states.AddStudentState
import com.github.heheteam.adminbot.states.AddTeacherState
import com.github.heheteam.adminbot.states.AskFirstNameState
import com.github.heheteam.adminbot.states.AskLastNameState
import com.github.heheteam.adminbot.states.ConfirmDeleteMessageState
import com.github.heheteam.adminbot.states.CourseInfoState
import com.github.heheteam.adminbot.states.CreateAssignmentErrorState
import com.github.heheteam.adminbot.states.CreateAssignmentState
import com.github.heheteam.adminbot.states.CreateCourseState
import com.github.heheteam.adminbot.states.EditCourseState
import com.github.heheteam.adminbot.states.MenuState
import com.github.heheteam.adminbot.states.PerformDeleteMessageState
import com.github.heheteam.adminbot.states.QueryCourseForEditing
import com.github.heheteam.adminbot.states.RemoveStudentState
import com.github.heheteam.adminbot.states.RemoveTeacherState
import com.github.heheteam.adminbot.states.StartState
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
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.interfaces.toAdminId
import com.github.heheteam.commonlib.interfaces.toStudentId
import com.github.heheteam.commonlib.state.BotStateWithHandlers
import com.github.heheteam.commonlib.state.SuspendableBotAction
import com.github.heheteam.commonlib.state.registerState
import com.github.heheteam.commonlib.state.registerStateForBotState
import com.github.heheteam.commonlib.util.ActionWrapper
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.UpdateHandlersController
import com.github.heheteam.commonlib.util.getCurrentMoscowTime
import com.github.heheteam.commonlib.util.startStateOnUnhandledUpdate
import com.github.michaelbull.result.get
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.error
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.bot.setMyCommands
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndFSMAndStartLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.command
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import dev.inmo.tgbotapi.extensions.utils.groupChatOrNull
import dev.inmo.tgbotapi.types.BotCommand
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.content.TextMessage
import dev.inmo.tgbotapi.utils.RiskFeature
import dev.inmo.tgbotapi.utils.buildEntities
import dev.inmo.tgbotapi.utils.code
import io.ktor.http.escapeIfNeeded
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.LocalDateTime

private const val DEFAULT_ADMIN_ID = 0L

class AdminRunner(private val adminApi: AdminApi) {
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
          if (user != null) startChain(StartState(user))
        }
        command("bind") { tryBindingChatToErrorService(it) }

        startStateOnUnhandledUpdate { user ->
          if (user != null) startChain(MenuState(user, DEFAULT_ADMIN_ID.toAdminId()))
        }

        registerAllStates(botToken)

        allUpdatesFlow.subscribeSafelyWithoutExceptions(this) {
          println(getCurrentMoscowTime().toString() + " " + it.toString().escapeIfNeeded())
        }
      }
      .second
      .join()
  }

  @OptIn(RiskFeature::class)
  private suspend fun DefaultBehaviourContextWithFSM<State>.tryBindingChatToErrorService(
    message: TextMessage
  ) {
    val chat = message.chat.groupChatOrNull() ?: return
    val user = message.from
    if (user != null && adminApi.tgIdIsInWhitelist(user.id).get() ?: false) {
      adminApi.bindErrorChat(chat.id.chatId)
      bot.send(chat, "This chat has been successfully bound!")
    } else {
      val text = buildEntities {
        +"Your Telegram ID ("
        code(user?.id.toString())
        +") is not in the whitelist."
      }
      bot.send(chat, text)
    }
  }

  private fun DefaultBehaviourContextWithFSM<State>.registerAllStates(botToken: String) {
    registerStateForBotState<StartState, AdminApi>(adminApi)
    registerStateForBotState<AskFirstNameState, AdminApi>(adminApi)
    registerState<AskLastNameState, AdminApi>(adminApi)
    registerStateForBotStateWithHandlers<QueryScheduledMessageUserGroupState>(::registerHandlers)
    registerStateForBotStateWithHandlers<CreateCourseState>(::registerHandlers)
    registerStateForBotStateWithHandlers<CreateAssignmentState>(::registerHandlers)
    registerStateForBotStateWithHandlers<CreateAssignmentErrorState>(::registerHandlers)
    registerStateForBotStateWithHandlers<AddAdminState>(::registerHandlers)
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
    registerStateForBotStateWithHandlers<QueryScheduledMessageDateState>(::registerHandlers)
    registerStateForBotStateWithHandlers<EnterScheduledMessageDateManuallyState>(::registerHandlers)
    registerStateForBotStateWithHandlers<QueryScheduledMessageTimeState>(::registerHandlers)
    registerStateForBotStateWithHandlers<ConfirmScheduledMessageState>(::registerHandlers)
    onStateOrSubstate<AdminHandleable> { it.handleAdmin(this, adminApi, ::registerHandlers) }
    strictlyOn<QueryScheduledMessageContentState> { state ->
      state.adminBotToken = botToken
      state.handle(this, adminApi, ::registerHandlers)
    }
  }

  private fun registerHandlers(
    handlersController: UpdateHandlersController<SuspendableBotAction, out Any?, FrontendError>,
    context: User,
  ) {
    addMenuCommandHandler(handlersController, context)
    addMoveDeadlinesHandler(handlersController, context)
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

  private fun addMoveDeadlinesHandler(
    handlersController: UpdateHandlersController<SuspendableBotAction, out Any?, FrontendError>,
    context: User,
  ) {
    handlersController.addDataCallbackHandler { dataCallbackQuery ->
      if (dataCallbackQuery.data.startsWith(AdminKeyboards.MOVE_DEADLINES)) {
        ActionWrapper {
          val args = dataCallbackQuery.data.drop(AdminKeyboards.MOVE_DEADLINES.length).split(' ')
          val studentId = args.getOrNull(1)?.toLongOrNull()?.toStudentId()
          val dateTimeString = args.getOrNull(2)
          if (studentId == null) {
            KSLog.error("Unexpected data callback query \"${dataCallbackQuery.data}\"")
          } else if (dateTimeString == null) {
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
}
