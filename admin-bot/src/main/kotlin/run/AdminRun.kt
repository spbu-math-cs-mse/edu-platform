package com.github.heheteam.adminbot.run

import com.github.heheteam.adminbot.AdminCore
import com.github.heheteam.adminbot.states.*
import com.github.heheteam.commonlib.api.AdminIdRegistry
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
suspend fun adminRun(botToken: String, adminIdRegistry: AdminIdRegistry, core: AdminCore) {
  telegramBot(botToken) {
    logger =
      KSLog { level: LogLevel, tag: String?, message: Any, throwable: Throwable? ->
        println(defaultMessageFormatter(level, tag, message, throwable))
      }
  }

  telegramBotWithBehaviourAndFSMAndStartLongPolling<BotState>(
    botToken,
    CoroutineScope(Dispatchers.IO),
    onStateHandlingErrorHandler = { state, e ->
      println("Thrown error on $state")
      e.printStackTrace()
      state
    },
  ) {
    println(getMe())

    command(
      "start",
    ) {
      startChain(StartState(it.from!!))
    }

    strictlyOnStartState(adminIdRegistry)
    strictlyOnMenuState()
    strictlyOnCreateCourseState(core)
    strictlyOnEditCourseState(core)
    strictlyOnAddStudentState(core)
    strictlyOnRemoveStudentState(core)
    strictlyOnAddTeacherState(core)
    strictlyOnRemoveTeacherState(core)
    strictlyOnEditDescriptionState()
    strictlyOnAddScheduledMessageState(core)
    strictlyOnGetTeachersState(core)
    strictlyOnGetProblemsState(core)
    strictlyOnCreateAssignmentState(core)

    allUpdatesFlow.subscribeSafelyWithoutExceptions(this) {
      println(it)
    }
  }.second.join()
}
