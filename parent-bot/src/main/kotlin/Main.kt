package com.github.heheteam.parentbot

import ParentCore
import com.github.heheteam.commonlib.mock.*
import com.github.heheteam.parentbot.states.*
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
import strictlyOnStartState

/**
 * @param args bot token and telegram @username for mocking data.
 */
@OptIn(RiskFeature::class)
suspend fun main(vararg args: String) {
  val botToken = args.first()
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
    val mockCoursesDistributor = MockCoursesDistributor()
    val userIdRegistry = MockParentIdRegistry(mockCoursesDistributor.singleUserId)
    val core =
      ParentCore(
        InMemoryStudentStorage(),
        InMemoryGradeTable(),
        InMemorySolutionDistributor(),
      )

    strictlyOnStartState(isDeveloperRun = true)
    strictlyOnMenuState(userIdRegistry, core)
    strictlyOnGivingFeedbackState(userIdRegistry)
    strictlyOnChildPerformanceState(userIdRegistry, core)

    allUpdatesFlow.subscribeSafelyWithoutExceptions(this) {
      println(it)
    }
  }.second.join()
}
