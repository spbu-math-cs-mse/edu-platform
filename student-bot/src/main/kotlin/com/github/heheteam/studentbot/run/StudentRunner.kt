package com.github.heheteam.studentbot.run

import com.github.heheteam.commonlib.api.CoursesDistributor
import com.github.heheteam.commonlib.api.ProblemStorage
import com.github.heheteam.commonlib.api.StudentStorage
import com.github.heheteam.commonlib.util.DeveloperOptions
import com.github.heheteam.commonlib.util.registerState
import com.github.heheteam.studentbot.StudentCore
import com.github.heheteam.studentbot.state.CheckDeadlinesState
import com.github.heheteam.studentbot.state.DeveloperStartState
import com.github.heheteam.studentbot.state.MenuState
import com.github.heheteam.studentbot.state.PresetStudentState
import com.github.heheteam.studentbot.state.StartState
import com.github.heheteam.studentbot.state.ViewState
import com.github.heheteam.studentbot.state.strictlyOnCheckGradesState
import com.github.heheteam.studentbot.state.strictlyOnPresetStudentState
import com.github.heheteam.studentbot.state.strictlyOnSendSolutionState
import com.github.heheteam.studentbot.state.strictlyOnSignUpState
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
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class StudentRunner : KoinComponent {
  private val studentStorage: StudentStorage by inject()
  private val coursesDistributor: CoursesDistributor by inject()
  private val problemStorage: ProblemStorage by inject()

  @OptIn(RiskFeature::class)
  suspend fun run(botToken: String, developerOptions: DeveloperOptions? = DeveloperOptions()) {
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

        val core = StudentCore()
        registerState<StartState, StudentStorage>(studentStorage)
        registerState<DeveloperStartState, StudentStorage>(studentStorage)
        registerState<MenuState, CoursesDistributor>(coursesDistributor)
        registerState<ViewState, CoursesDistributor>(coursesDistributor)
        strictlyOnSignUpState(core)
        strictlyOnSendSolutionState(core, botToken)
        strictlyOnCheckGradesState(core)
        strictlyOnPresetStudentState(core)
        registerState<CheckDeadlinesState, ProblemStorage>(problemStorage)

        allUpdatesFlow.subscribeSafelyWithoutExceptions(this) { println(it) }
      }
      .second
      .join()
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
}
