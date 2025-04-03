package com.github.heheteam.adminbot

import com.github.heheteam.adminbot.states.AddStudentState
import com.github.heheteam.adminbot.states.AddTeacherState
import com.github.heheteam.adminbot.states.CourseInfoState
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
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndFSMAndStartLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.command
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import dev.inmo.tgbotapi.utils.RiskFeature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

@OptIn(RiskFeature::class)
suspend fun adminRun(botToken: String, adminApi: AdminApi) {
  telegramBot(botToken) {
    logger = KSLog { level: LogLevel, tag: String?, message: Any, throwable: Throwable? ->
      println(defaultMessageFormatter(level, tag, message, throwable))
    }
  }

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

      command("start") {
        val user = it.from
        if (user != null) startChain(MenuState(user))
      }

      registerState<MenuState, AdminApi>(adminApi)
      registerState<CreateCourseState, AdminApi>(adminApi)
      registerState<CourseInfoState, AdminApi>(adminApi)
      registerState<EditCourseState, Unit>(Unit)
      registerState<EditDescriptionState, Unit>(Unit)
      registerState<CreateAssignmentState, AdminApi>(adminApi)
      registerState<AddStudentState, AdminApi>(adminApi)
      registerState<RemoveStudentState, AdminApi>(adminApi)
      registerState<AddTeacherState, AdminApi>(adminApi)
      registerState<RemoveTeacherState, AdminApi>(adminApi)
      registerState<QueryCourseForEditing, AdminApi>(adminApi)
      strictlyOnAddScheduledMessageState(adminApi)

      allUpdatesFlow.subscribeSafelyWithoutExceptions(this) { println(it) }
    }
    .second
    .join()
}
