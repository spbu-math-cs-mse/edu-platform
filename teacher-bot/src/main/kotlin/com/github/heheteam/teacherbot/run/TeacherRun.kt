package com.github.heheteam.teacherbot.run

import com.github.heheteam.commonlib.api.CoursesDistributor
import com.github.heheteam.commonlib.api.RedisBotEventBus
import com.github.heheteam.commonlib.api.TeacherStatistics
import com.github.heheteam.commonlib.api.TeacherStorage
import com.github.heheteam.commonlib.util.DeveloperOptions
import com.github.heheteam.commonlib.util.registerState
import com.github.heheteam.teacherbot.CoursesStatisticsResolver
import com.github.heheteam.teacherbot.SolutionAssessor
import com.github.heheteam.teacherbot.SolutionResolver
import com.github.heheteam.teacherbot.states.CheckGradesState
import com.github.heheteam.teacherbot.states.ChooseGroupCourseState
import com.github.heheteam.teacherbot.states.DeveloperStartState
import com.github.heheteam.teacherbot.states.GettingSolutionState
import com.github.heheteam.teacherbot.states.GradingSolutionState
import com.github.heheteam.teacherbot.states.ListeningForSolutionsGroupState
import com.github.heheteam.teacherbot.states.MenuState
import com.github.heheteam.teacherbot.states.PresetTeacherState
import com.github.heheteam.teacherbot.states.SendStatisticInfoState
import com.github.heheteam.teacherbot.states.StartState
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndFSMAndStartLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.command
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import dev.inmo.tgbotapi.extensions.utils.groupContentMessageOrNull
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
  botEventBus: RedisBotEventBus,
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
        val groupContent = it.groupContentMessageOrNull()
        if (groupContent != null) {
          println(groupContent)
          sendMessage(groupContent.chat, "greetings!")
          startChain(ChooseGroupCourseState(groupContent.chat))
        } else if (user != null) {
          val startingState = findStartState(developerOptions, user)
          startChain(startingState)
        }
      }

      internalCompilerErrorWorkaround(solutionResolver, solutionAssessor, botEventBus)
      registerState<StartState, TeacherStorage>(teacherStorage)
      registerState<DeveloperStartState, TeacherStorage>(teacherStorage)
      registerState<MenuState, TeacherStatistics>(teacherStatistics)
      registerState<SendStatisticInfoState, TeacherStatistics>(teacherStatistics)
      registerState<CheckGradesState, CoursesStatisticsResolver>(coursesStatisticsResolver)
      registerState<GettingSolutionState, SolutionResolver>(solutionResolver)
      registerState<GradingSolutionState, SolutionAssessor>(solutionAssessor)
      registerState<PresetTeacherState, CoursesDistributor>(coursesDistributor)
      registerState<ChooseGroupCourseState, Unit>(Unit)

      allUpdatesFlow.subscribeSafelyWithoutExceptions(this) { println(it) }
    }
    .second
    .join()
}

private fun DefaultBehaviourContextWithFSM<State>.internalCompilerErrorWorkaround(
  solutionResolver: SolutionResolver,
  solutionAssessor: SolutionAssessor,
  botEventBus: RedisBotEventBus,
) {
  strictlyOn<ListeningForSolutionsGroupState>(
    registerState(solutionResolver, solutionAssessor, botEventBus)
  )
}

private fun registerState(
  solutionResolver: SolutionResolver,
  solutionAssessor: SolutionAssessor,
  botEventBus: RedisBotEventBus,
): suspend BehaviourContextWithFSM<in State>.(state: ListeningForSolutionsGroupState) -> State? =
  { state ->
    state.execute(
      this,
      solutionResolver,
      solutionResolver.solutionDistributor,
      solutionAssessor,
      botEventBus,
    )
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
