package com.github.heheteam.adminbot

import DatabaseCoursesDistributor
import com.github.heheteam.adminbot.states.*
import com.github.heheteam.commonlib.database.DatabaseStudentStorage
import com.github.heheteam.commonlib.database.DatabaseTeacherStorage
import com.github.heheteam.commonlib.mock.InMemoryScheduledMessagesDistributor
import com.github.heheteam.commonlib.mock.MockAdminIdRegistry
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
import org.jetbrains.exposed.sql.Database

@OptIn(RiskFeature::class)
suspend fun main(vararg args: String) {
  val botToken = args.first()
  telegramBot(botToken) {
    logger =
      KSLog { level: LogLevel, tag: String?, message: Any, throwable: Throwable? ->
        println(defaultMessageFormatter(level, tag, message, throwable))
      }
  }

  val database =
    Database.connect(
      "jdbc:h2:./data/films",
      driver = "org.h2.Driver",
    )

  val userIdRegistry = MockAdminIdRegistry(0L)
  val core =
    AdminCore(
      InMemoryScheduledMessagesDistributor(),
      DatabaseCoursesDistributor(database),
      DatabaseStudentStorage(database),
      DatabaseTeacherStorage(database),
    )

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

    strictlyOnStartState(userIdRegistry)
    strictlyOnMenuState()
    strictlyOnCreateCourseState(core)
    strictlyOnEditCourseState(core)
    strictlyOnAddStudentState(core)
    strictlyOnRemoveStudentState(core)
    strictlyOnAddTeacherState(core)
    strictlyOnRemoveTeacherState(core)
    strictlyOnEditDescriptionState()
    strictlyOnAddScheduledMessageState(core)

    allUpdatesFlow.subscribeSafelyWithoutExceptions(this) {
      println(it)
    }
  }.second.join()
}
