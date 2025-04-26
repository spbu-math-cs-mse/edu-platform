package com.github.heheteam.adminbot

import com.github.heheteam.adminbot.states.AddStudentState
import com.github.heheteam.adminbot.states.AddTeacherState
import com.github.heheteam.adminbot.states.CourseInfoState
import com.github.heheteam.adminbot.states.CreateAssignmentErrorState
import com.github.heheteam.adminbot.states.CreateAssignmentState
import com.github.heheteam.adminbot.states.CreateCourseState
import com.github.heheteam.adminbot.states.EditCourseState
import com.github.heheteam.adminbot.states.EditDescriptionState
import com.github.heheteam.adminbot.states.MenuState
import com.github.heheteam.adminbot.states.QueryCourseForEditing
import com.github.heheteam.adminbot.states.RemoveStudentState
import com.github.heheteam.adminbot.states.RemoveTeacherState
import com.github.heheteam.adminbot.states.strictlyOnAddScheduledMessageState
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.state.BotStateWithHandlers
import com.github.heheteam.commonlib.state.registerState
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.UpdateHandlersController
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.bot.setMyCommands
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndFSMAndStartLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.command
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import dev.inmo.tgbotapi.types.BotCommand
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.utils.RiskFeature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class AdminRunner(val adminApi: AdminApi) {
  private inline fun <reified S : BotStateWithHandlers<*, *, AdminApi>> DefaultBehaviourContextWithFSM<
    State
  >
    .registerState(
    noinline initUpdateHandlers:
      (UpdateHandlersController<() -> Unit, out Any?, Any>, context: User) -> Unit =
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
          println("Thrown error on $state")
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
          if (user != null) startChain(MenuState(user))
        }

        registerState<MenuState>(::menuCommandHandler)
        registerState<CreateCourseState>(::menuCommandHandler)
        registerState<CreateAssignmentState>(::menuCommandHandler)
        registerState<CreateAssignmentErrorState>(::menuCommandHandler)
        registerState<AddStudentState>(::menuCommandHandler)
        registerState<RemoveStudentState>(::menuCommandHandler)
        registerState<AddTeacherState>(::menuCommandHandler)
        registerState<RemoveTeacherState>(::menuCommandHandler)
        registerState<QueryCourseForEditing>(::menuCommandHandler)
        registerState<EditCourseState, Unit>(Unit, ::menuCommandHandler)
        registerState<CourseInfoState, AdminApi>(adminApi)
        registerState<EditDescriptionState, Unit>(Unit)
        strictlyOnAddScheduledMessageState(adminApi)

        allUpdatesFlow.subscribeSafelyWithoutExceptions(this) { println(it) }
      }
      .second
      .join()
  }

  private fun menuCommandHandler(
    handlersController: UpdateHandlersController<() -> Unit, out Any?, Any>,
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
}
