package com.github.heheteam.studentbot

import DatabaseCoursesDistributor
import com.github.heheteam.commonlib.api.AssignmentStorage
import com.github.heheteam.commonlib.api.ProblemStorage
import com.github.heheteam.commonlib.database.*
import com.github.heheteam.commonlib.mock.*
import com.github.heheteam.commonlib.util.fillWithSamples
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
    val database = Database.connect(
      "jdbc:h2:./data/films",
      driver = "org.h2.Driver",
    )
    val studentStorage = DatabaseStudentStorage(database)
    val coursesDistributor = DatabaseCoursesDistributor(database)
    val problemStorage: ProblemStorage = DatabaseProblemStorage(database)
    val assignmentStorage: AssignmentStorage = DatabaseAssignmentStorage(database)
    fillWithSamples(coursesDistributor, problemStorage, assignmentStorage, studentStorage)
    val userIdRegistry = MockStudentIdRegistry(1L)
    val solutionDistributor = DatabaseSolutionDistributor(database)
    val core =
      StudentCore(
        solutionDistributor,
        coursesDistributor,
        problemStorage,
        assignmentStorage,
        DatabaseGradeTable(database),
      )

    strictlyOnStartState(userIdRegistry, isDeveloperRun = true)
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
