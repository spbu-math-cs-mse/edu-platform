package com.github.heheteam.studentbot

import com.github.heheteam.studentbot.data.MockCoursesDistributor
import com.github.heheteam.studentbot.state.* 
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
suspend fun main(vararg args: String) {
  val coursesDistributor = MockCoursesDistributor()
  val botToken = args.first()
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
      if (it.from != null) {
        startChain(StartState(it.from!!))
      }
    }
    
    strictlyOnStartState()
    strictlyOnMenuState()
    strictlyOnViewState(coursesDistributor)
    strictlyOnSignUpState(coursesDistributor)
    strictlyOnSendSolutionState(coursesDistributor)

    allUpdatesFlow.subscribeSafelyWithoutExceptions(this) {
      println(it)
    }
  }.second.join()
}
