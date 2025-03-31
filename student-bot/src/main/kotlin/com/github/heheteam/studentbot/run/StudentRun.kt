package com.github.heheteam.studentbot.run

import com.github.heheteam.commonlib.api.ProblemStorage
import com.github.heheteam.commonlib.api.StudentStorage
import com.github.heheteam.commonlib.state.registerState
import com.github.heheteam.commonlib.util.DeveloperOptions
import com.github.heheteam.studentbot.StudentApi
import com.github.heheteam.studentbot.state.CheckDeadlinesState
import com.github.heheteam.studentbot.state.ConfirmSubmissionState
import com.github.heheteam.studentbot.state.DeveloperStartState
import com.github.heheteam.studentbot.state.MenuState
import com.github.heheteam.studentbot.state.PresetStudentState
import com.github.heheteam.studentbot.state.QueryAssignmentForCheckingGradesState
import com.github.heheteam.studentbot.state.QueryCourseForCheckingDeadlinesState
import com.github.heheteam.studentbot.state.QueryCourseForSolutionSendingState
import com.github.heheteam.studentbot.state.QueryProblemForSolutionSendingState
import com.github.heheteam.studentbot.state.SendSolutionState
import com.github.heheteam.studentbot.state.StartState
import com.github.heheteam.studentbot.state.strictlyOnPresetStudentState
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndFSMAndStartLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.command
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.utils.RiskFeature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

@OptIn(RiskFeature::class)
suspend fun studentRun(
  botToken: String,
  studentStorage: StudentStorage,
  problemStorage: ProblemStorage,
  studentApi: StudentApi,
  developerOptions: DeveloperOptions? = DeveloperOptions(),
) {
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

      registerState<StartState, StudentStorage>(studentStorage)
      registerState<DeveloperStartState, StudentStorage>(studentStorage)
      registerState<MenuState, StudentApi>(studentApi)
      registerState<ConfirmSubmissionState, StudentApi>(studentApi)
      registerSendSolutionState(botToken, studentApi)
      registerState<QueryCourseForSolutionSendingState, StudentApi>(studentApi)
      registerState<QueryCourseForCheckingDeadlinesState, StudentApi>(studentApi)
      registerState<QueryAssignmentForCheckingGradesState, StudentApi>(studentApi)
      strictlyOnPresetStudentState(studentApi)
      registerState<CheckDeadlinesState, ProblemStorage>(problemStorage)
      registerState<QueryCourseForSolutionSendingState, StudentApi>(studentApi) {}
      registerState<QueryProblemForSolutionSendingState, StudentApi>(studentApi) {}

      allUpdatesFlow.subscribeSafelyWithoutExceptions(this) { println(it) }
    }
    .second
    .join()
}

private fun DefaultBehaviourContextWithFSM<State>.registerSendSolutionState(
  botToken: String,
  studentApi: StudentApi,
) {
  strictlyOn<SendSolutionState> { state ->
    state.studentBotToken = botToken
    state.handle(this, studentApi) {}
  }
}

private fun findStartState(developerOptions: DeveloperOptions?, user: User) =
  if (developerOptions != null) {
    val presetStudent = developerOptions.presetStudentId
    if (presetStudent != null) {
      PresetStudentState(user, presetStudent)
    } else {
      DeveloperStartState(user)
    }
  } else {
    StartState(user)
  }
