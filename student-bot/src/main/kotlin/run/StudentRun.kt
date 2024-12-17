package com.github.heheteam.studentbot.run

import com.github.heheteam.commonlib.api.StudentStorage
import com.github.heheteam.studentbot.StudentCore
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
suspend fun studentRun(
  botToken: String,
  studentStorage: StudentStorage,
  core: StudentCore,
  isDeveloperRun: Boolean = true,
) {
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
      val user = it.from
      if (user != null) {
        if (isDeveloperRun) {
          startChain(DevStartState(user))
        } else {
          startChain(StartState(user))
        }
      }
    }

    strictlyOnStartState(studentStorage)
    strictlyOnDeveloperStartState(studentStorage)
    strictlyOnMenuState()
    strictlyOnViewState(core)
    strictlyOnSignUpState(core)
    strictlyOnSendSolutionState(core)
    strictlyOnCheckGradesState(core)

    allUpdatesFlow.subscribeSafelyWithoutExceptions(this) {
      println(it)
    }
  }.second.join()
}
