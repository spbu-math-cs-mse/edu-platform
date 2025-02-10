package com.github.heheteam.teacherbot.run

import com.github.heheteam.commonlib.api.CoursesDistributor
import com.github.heheteam.commonlib.api.TeacherStatistics
import com.github.heheteam.commonlib.api.TeacherStorage
import com.github.heheteam.commonlib.util.DeveloperOptions
import com.github.heheteam.commonlib.util.registerState
import com.github.heheteam.teacherbot.CoursesStatisticsResolver
import com.github.heheteam.teacherbot.SolutionAssessor
import com.github.heheteam.teacherbot.SolutionResolver
import com.github.heheteam.teacherbot.states.CheckGradesState
import com.github.heheteam.teacherbot.states.DeveloperStartState
import com.github.heheteam.teacherbot.states.GettingSolutionState
import com.github.heheteam.teacherbot.states.GradingSolutionState
import com.github.heheteam.teacherbot.states.MenuState
import com.github.heheteam.teacherbot.states.PresetTeacherState
import com.github.heheteam.teacherbot.states.SendStatisticInfoState
import com.github.heheteam.teacherbot.states.StartState
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndFSMAndStartLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.command
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.utils.RiskFeature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

@OptIn(RiskFeature::class)
suspend fun teacherRun(
  botToken: String,
  teacherStorage: TeacherStorage,
  teacherStatistics: TeacherStatistics,
  coursesDistributor: CoursesDistributor,
  coursesStatisticsResolver: CoursesStatisticsResolver,
  solutionResolver: SolutionResolver,
  solutionAssessor: SolutionAssessor,
  developerOptions: DeveloperOptions? = DeveloperOptions(),
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

      command("start") {
        val user = it.from
        if (user != null) {
          val startingState = findStartState(developerOptions, user)
          startChain(startingState)
        }
      }

      registerState<StartState, TeacherStorage>(teacherStorage)
      registerState<DeveloperStartState, TeacherStorage>(teacherStorage)
      registerState<MenuState, TeacherStatistics>(teacherStatistics)
      registerState<SendStatisticInfoState, TeacherStatistics>(teacherStatistics)
      registerState<CheckGradesState, CoursesStatisticsResolver>(coursesStatisticsResolver)
      registerState<GettingSolutionState, SolutionResolver>(solutionResolver)
      registerState<GradingSolutionState, SolutionAssessor>(solutionAssessor)
      registerState<PresetTeacherState, CoursesDistributor>(coursesDistributor)

      allUpdatesFlow.subscribeSafelyWithoutExceptions(this) { println(it) }
    }
    .second
    .join()
}

private fun findStartState(developerOptions: DeveloperOptions?, user: User) =
  if (developerOptions != null) {
    val presetTeacher = developerOptions.presetTeacherId
    if (presetTeacher != null) {
      PresetTeacherState(user, presetTeacher)
    } else {
      DeveloperStartState(user)
    }
  } else {
    StartState(user)
  }
