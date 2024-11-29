package com.github.heheteam.teacherbot

import DatabaseCoursesDistributor
import com.github.heheteam.commonlib.database.DatabaseGradeTable
import com.github.heheteam.commonlib.database.DatabaseSolutionDistributor
import com.github.heheteam.commonlib.mock.*
import com.github.heheteam.teacherbot.state.strictlyOnCheckGradesState
import com.github.heheteam.teacherbot.states.*
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
    val database = Database.connect(
      "jdbc:h2:./data/films",
      driver = "org.h2.Driver",
    )
    val coursesDistributor = DatabaseCoursesDistributor(database)
    val userIdRegistry = MockTeacherIdRegistry(0L)
    val inMemoryTeacherStatistics = InMemoryTeacherStatistics()
    val core =
      TeacherCore(
        inMemoryTeacherStatistics,
        coursesDistributor,
        DatabaseSolutionDistributor(database),
        DatabaseGradeTable(database),
      )

    strictlyOnStartState(userIdRegistry)
    strictlyOnMenuState(userIdRegistry, core)
    strictlyOnGettingSolutionState(userIdRegistry, core)
    strictlyOnCheckGradesState(userIdRegistry, core)

    allUpdatesFlow.subscribeSafelyWithoutExceptions(this) {
      println(it)
    }
  }.second.join()
}
