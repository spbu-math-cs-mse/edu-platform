package com.github.heheteam.adminbot.run

import com.github.heheteam.adminbot.AdminCore
import com.github.heheteam.adminbot.AssignmentCreator
import com.github.heheteam.adminbot.CourseStatisticsComposer
import com.github.heheteam.adminbot.states.AddStudentState
import com.github.heheteam.adminbot.states.AddTeacherState
import com.github.heheteam.adminbot.states.CourseInfoState
import com.github.heheteam.adminbot.states.CreateAssignmentState
import com.github.heheteam.adminbot.states.CreateCourseState
import com.github.heheteam.adminbot.states.EditCourseState
import com.github.heheteam.adminbot.states.EditDescriptionState
import com.github.heheteam.adminbot.states.MenuState
import com.github.heheteam.adminbot.states.QueryCourseForEditing
import com.github.heheteam.adminbot.states.RemoveStudentState
import com.github.heheteam.adminbot.states.RemoveTeacherState
import com.github.heheteam.adminbot.states.strictlyOnAddScheduledMessageState
import com.github.heheteam.commonlib.api.AssignmentStorage
import com.github.heheteam.commonlib.api.CoursesDistributor
import com.github.heheteam.commonlib.api.ProblemStorage
import com.github.heheteam.commonlib.api.SolutionDistributor
import com.github.heheteam.commonlib.state.registerState
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
suspend fun adminRun(
  botToken: String,
  coursesDistributor: CoursesDistributor,
  assignmentStorage: AssignmentStorage,
  problemStorage: ProblemStorage,
  solutionDistributor: SolutionDistributor,
  core: AdminCore,
) {
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
      println(getMe())

      command("start") { startChain(MenuState(it.from!!)) }

      registerState<MenuState, CoursesDistributor>(coursesDistributor)
      registerState<CreateCourseState, CoursesDistributor>(coursesDistributor)
      registerState<CourseInfoState, CourseStatisticsComposer>(
        CourseStatisticsComposer(
          coursesDistributor,
          assignmentStorage,
          problemStorage,
          solutionDistributor,
        )
      )
      registerState<EditCourseState, Unit>(Unit)
      registerState<EditDescriptionState, Unit>(Unit)
      registerState<CreateAssignmentState, AssignmentCreator>(
        AssignmentCreator(assignmentStorage, problemStorage)
      )
      registerState<AddStudentState, AdminCore>(core)
      registerState<RemoveStudentState, AdminCore>(core)
      registerState<AddTeacherState, AdminCore>(core)
      registerState<RemoveTeacherState, AdminCore>(core)
      registerState<QueryCourseForEditing, AdminCore>(core)
      strictlyOnAddScheduledMessageState(core)

      allUpdatesFlow.subscribeSafelyWithoutExceptions(this) { println(it) }
    }
    .second
    .join()
}
