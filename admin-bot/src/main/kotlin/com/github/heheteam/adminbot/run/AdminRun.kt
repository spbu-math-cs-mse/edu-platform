package com.github.heheteam.adminbot.run

import com.github.heheteam.adminbot.AdminCore
import com.github.heheteam.adminbot.AssignmentCreator
import com.github.heheteam.adminbot.AssignmentProblemsResolver
import com.github.heheteam.adminbot.CourseStatisticsComposer
import com.github.heheteam.adminbot.states.CourseInfoState
import com.github.heheteam.adminbot.states.CreateAssignmentState
import com.github.heheteam.adminbot.states.CreateCourseState
import com.github.heheteam.adminbot.states.EditCourseState
import com.github.heheteam.adminbot.states.EditDescriptionState
import com.github.heheteam.adminbot.states.GetProblemsState
import com.github.heheteam.adminbot.states.GetTeachersState
import com.github.heheteam.adminbot.states.MenuState
import com.github.heheteam.adminbot.states.strictlyOnAddScheduledMessageState
import com.github.heheteam.adminbot.states.strictlyOnAddStudentState
import com.github.heheteam.adminbot.states.strictlyOnAddTeacherState
import com.github.heheteam.adminbot.states.strictlyOnRemoveStudentState
import com.github.heheteam.adminbot.states.strictlyOnRemoveTeacherState
import com.github.heheteam.commonlib.api.AssignmentStorage
import com.github.heheteam.commonlib.api.CoursesDistributor
import com.github.heheteam.commonlib.api.ProblemStorage
import com.github.heheteam.commonlib.api.SolutionDistributor
import com.github.heheteam.commonlib.api.TeacherStorage
import com.github.heheteam.commonlib.util.registerState
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
  teacherStorage: TeacherStorage,
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
      registerState<GetTeachersState, TeacherStorage>(teacherStorage)
      registerState<GetProblemsState, AssignmentProblemsResolver>(
        AssignmentProblemsResolver(assignmentStorage, problemStorage)
      )
      registerState<CreateAssignmentState, AssignmentCreator>(
        AssignmentCreator(assignmentStorage, problemStorage)
      )
      strictlyOnAddStudentState(core)
      strictlyOnRemoveStudentState(core)
      strictlyOnAddTeacherState(core)
      strictlyOnRemoveTeacherState(core)
      strictlyOnAddScheduledMessageState(core)

      allUpdatesFlow.subscribeSafelyWithoutExceptions(this) { println(it) }
    }
    .second
    .join()
}
