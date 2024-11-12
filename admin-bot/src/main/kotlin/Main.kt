package com.github.heheteam.adminbot

import com.github.heheteam.adminbot.states.*
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndFSMAndStartLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.command
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

suspend fun main(vararg args: String) {
  val botToken = args.first()
  mockTgUsername = args[1]
  val bot =
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

    strictlyOnStartState()
    strictlyOnNotAdminState()
    strictlyOnMenuState()
    strictlyOnCreateCourseState()
    strictlyOnPickACourseState()
    strictlyOnEditCourseState()
    strictlyOnAddStudentState()
    strictlyOnRemoveStudentState()
    strictlyOnAddTeacherState()
    strictlyOnRemoveTeacherState()
    strictlyOnEditDescriptionState()
    strictlyOnAddScheduledMessageState()
    strictlyOnScheduleMessageSelectDateState()
    strictlyOnScheduleMessageEnterDateState()
    strictlyOnScheduleMessageEnterTimeState()

    allUpdatesFlow.subscribeSafelyWithoutExceptions(this) {
      println(it)
    }
  }.second.join()
}
