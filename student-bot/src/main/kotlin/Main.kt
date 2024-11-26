package com.github.heheteam.studentbot

import com.github.heheteam.commonlib.mock.MockCoursesDistributor
import com.github.heheteam.commonlib.mock.MockGradeTable
import com.github.heheteam.commonlib.mock.MockSolutionDistributor
import com.github.heheteam.commonlib.mock.MockUserIdRegistry
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
    val userIdRegistry = MockUserIdRegistry(mockCoursesDistributor.singleUserId)
    val core =
      StudentCore(
        MockSolutionDistributor(),
        mockCoursesDistributor,
      )
    run {
      // fill with mock data
      val firstCourse = core.getStudentCourses(mockCoursesDistributor.singleUserId).first()
      val firstAssignment = firstCourse.assignments.first()
      (firstCourse.gradeTable as MockGradeTable).addMockFilling(
        firstAssignment,
        mockCoursesDistributor.singleUserId,
      )
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
