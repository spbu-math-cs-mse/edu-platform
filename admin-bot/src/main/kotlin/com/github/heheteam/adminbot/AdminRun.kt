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
import com.github.heheteam.commonlib.state.registerState
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.UpdateHandlersController
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.bot.setMyCommands
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndFSMAndStartLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.command
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import dev.inmo.tgbotapi.types.BotCommand
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.utils.RiskFeature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

@OptIn(RiskFeature::class)
suspend fun adminRun(botToken: String, adminApi: AdminApi) {
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

      registerState<MenuState, AdminApi>(adminApi, ::menuCommandHandler)
      registerState<CreateCourseState, AdminApi>(adminApi, ::menuCommandHandler)
      registerState<EditCourseState, Unit>(Unit, ::menuCommandHandler)
      registerState<CreateAssignmentState, AdminApi>(adminApi, ::menuCommandHandler)
      registerState<CreateAssignmentErrorState, AdminApi>(adminApi, ::menuCommandHandler)
      registerState<AddStudentState, AdminApi>(adminApi, ::menuCommandHandler)
      registerState<RemoveStudentState, AdminApi>(adminApi, ::menuCommandHandler)
      registerState<AddTeacherState, AdminApi>(adminApi, ::menuCommandHandler)
      registerState<RemoveTeacherState, AdminApi>(adminApi, ::menuCommandHandler)
      registerState<QueryCourseForEditing, AdminApi>(adminApi, ::menuCommandHandler)
      registerState<CourseInfoState, AdminApi>(adminApi)
      registerState<EditDescriptionState, Unit>(Unit)
      strictlyOnAddScheduledMessageState(adminApi)

      allUpdatesFlow.subscribeSafelyWithoutExceptions(this) { println(it) }
    }
    .second
    .join()
}

fun menuCommandHandler(
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
