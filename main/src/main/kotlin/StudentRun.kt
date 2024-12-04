package com.github.heheteam

import com.github.heheteam.commonlib.api.StudentIdRegistry
import com.github.heheteam.studentbot.StudentCore
import com.github.heheteam.studentbot.state.StartState
import com.github.heheteam.studentbot.state.strictlyOnCheckGradesState
import com.github.heheteam.studentbot.state.strictlyOnMenuState
import com.github.heheteam.studentbot.state.strictlyOnSendSolutionState
import com.github.heheteam.studentbot.state.strictlyOnSignUpState
import com.github.heheteam.studentbot.state.strictlyOnStartState
import com.github.heheteam.studentbot.state.strictlyOnViewState
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
suspend fun studentRun(botToken: String, userIdRegistry: StudentIdRegistry, core: StudentCore) {
  telegramBot(botToken) {
    logger =
      KSLog { level: LogLevel, tag: String?, message: Any, throwable: Throwable? ->
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
      if (it.from != null) {
        startChain(StartState(it.from!!))
      }
    }

    strictlyOnStartState(isDeveloperRun = true)
    strictlyOnMenuState()
    strictlyOnViewState(userIdRegistry, core)
    strictlyOnSignUpState(userIdRegistry, core)
    strictlyOnSendSolutionState(userIdRegistry, core)
    strictlyOnCheckGradesState(userIdRegistry, core)

    allUpdatesFlow.subscribeSafelyWithoutExceptions(this) {
      println(it)
    }
  }.second.join()
}
