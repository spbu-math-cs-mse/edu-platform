package com.github.heheteam

import com.github.heheteam.commonlib.api.ParentIdRegistry
import com.github.heheteam.parentbot.ParentCore
import com.github.heheteam.parentbot.states.StartState
import com.github.heheteam.parentbot.states.strictlyOnChildPerformanceState
import com.github.heheteam.parentbot.states.strictlyOnGivingFeedbackState
import com.github.heheteam.parentbot.states.strictlyOnMenuState
import com.github.heheteam.parentbot.states.strictlyOnStartState
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
suspend fun parentRun(botToken: String, userIdRegistry: ParentIdRegistry, core: ParentCore) {
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
    strictlyOnMenuState(userIdRegistry, core)
    strictlyOnGivingFeedbackState(userIdRegistry)
    strictlyOnChildPerformanceState(userIdRegistry, core)

    allUpdatesFlow.subscribeSafelyWithoutExceptions(this) {
      println(it)
    }
  }.second.join()
}
