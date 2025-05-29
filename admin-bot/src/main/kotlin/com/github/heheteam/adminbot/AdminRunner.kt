package com.github.heheteam.adminbot

import com.github.heheteam.adminbot.states.AddStudentState
import com.github.heheteam.adminbot.states.AddTeacherState
import com.github.heheteam.adminbot.states.CourseInfoState
import com.github.heheteam.adminbot.states.CreateAssignmentErrorState
import com.github.heheteam.adminbot.states.CreateAssignmentState
import com.github.heheteam.adminbot.states.CreateCourseState
import com.github.heheteam.adminbot.states.DeveloperStartState
import com.github.heheteam.adminbot.states.EditCourseState
import com.github.heheteam.adminbot.states.EditDescriptionState
import com.github.heheteam.adminbot.states.MenuState
import com.github.heheteam.adminbot.states.QueryCourseForEditing
import com.github.heheteam.adminbot.states.RemoveStudentState
import com.github.heheteam.adminbot.states.RemoveTeacherState
import com.github.heheteam.adminbot.states.strictlyOnAddScheduledMessageState
import com.github.heheteam.commonlib.EduPlatformError
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.interfaces.toStudentId
import com.github.heheteam.commonlib.state.BotStateWithHandlers
import com.github.heheteam.commonlib.state.registerState
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

class AdminRunner(private val adminApi: AdminApi) {
  private inline fun <
    reified S : BotStateWithHandlers<*, *, AdminApi>
  > DefaultBehaviourContextWithFSM<State>.registerState(
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
          if (user != null) startChain(DeveloperStartState(user))
        }

        registerState<MenuState>(::registerHandlers)
        registerState<CreateCourseState>(::registerHandlers)
        registerState<CreateAssignmentState>(::registerHandlers)
        registerState<CreateAssignmentErrorState>(::registerHandlers)
        registerState<AddStudentState>(::registerHandlers)
        registerState<RemoveStudentState>(::registerHandlers)
        registerState<AddTeacherState>(::registerHandlers)
        registerState<RemoveTeacherState>(::registerHandlers)
        registerState<QueryCourseForEditing>(::registerHandlers)
        registerState<EditCourseState, Unit>(Unit, ::registerHandlers)
        registerState<CourseInfoState, AdminApi>(adminApi, ::registerHandlers)

        registerState<DeveloperStartState, AdminApi>(adminApi)
        registerState<CourseInfoState, AdminApi>(adminApi)
        registerState<EditDescriptionState, Unit>(Unit)
        strictlyOnAddScheduledMessageState(adminApi)

        allUpdatesFlow.subscribeSafelyWithoutExceptions(this) { println(it) }
      }
      .second
      .join()
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
        NewState(MenuState(context))
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
