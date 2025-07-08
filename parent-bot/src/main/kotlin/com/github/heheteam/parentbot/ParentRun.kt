package com.github.heheteam.parentbot

import com.github.heheteam.commonlib.api.ParentApi
import com.github.heheteam.commonlib.state.registerStateWithParentId
import com.github.heheteam.commonlib.util.startStateOnUnhandledUpdate
import com.github.heheteam.parentbot.state.AddChildById
import com.github.heheteam.parentbot.state.Menu
import com.github.heheteam.parentbot.state.QueryCourseForStudentPerformance
import com.github.heheteam.parentbot.state.QueryStudentPerformance
import com.github.heheteam.parentbot.state.RegisterParent
import com.github.heheteam.parentbot.state.Start
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.strictlyOn
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndFSMAndStartLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.command
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import dev.inmo.tgbotapi.utils.RiskFeature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

@OptIn(RiskFeature::class)
suspend fun parentRun(botToken: String, parentApi: ParentApi) {
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
      //      println(getMe())

      command("start") {
        val user = it.from
        if (user != null) {
          startChain(Start(user))
        }
      }
      startStateOnUnhandledUpdate { user ->
        if (user != null) {
          startChain(Start(user))
        }
      }
      registerStates(parentApi)
      //      registerStateWithParentId<RegisterParent, ParentApi>(parentApi)

      allUpdatesFlow.subscribeSafelyWithoutExceptions(this) { println(it) }
    }
    .second
    .join()
}

private fun DefaultBehaviourContextWithFSM<State>.registerStates(parentApi: ParentApi) {
  strictlyOn<Start> { it.handle(this, parentApi) }
  registerStateWithParentId<Menu, ParentApi>(parentApi)
  registerStateWithParentId<QueryCourseForStudentPerformance, ParentApi>(parentApi)
  registerStateWithParentId<QueryStudentPerformance, ParentApi>(parentApi)
  strictlyOn<AddChildById> { it.handle(this, parentApi) }
  strictlyOn<RegisterParent> { it.handle(this, parentApi) }
}
