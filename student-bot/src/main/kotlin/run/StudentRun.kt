package com.github.heheteam.studentbot.run

import com.github.heheteam.commonlib.api.StudentIdRegistry
import com.github.heheteam.commonlib.api.StudentStorage
import com.github.heheteam.studentbot.StudentCore
import com.github.heheteam.studentbot.state.StartState
import com.github.heheteam.studentbot.state.strictlyOnCheckGradesState
import com.github.heheteam.studentbot.state.strictlyOnMenuState
import com.github.heheteam.studentbot.state.strictlyOnSendSolutionState
import com.github.heheteam.studentbot.state.strictlyOnSignUpState
import com.github.heheteam.studentbot.state.strictlyOnStartState
import com.github.heheteam.studentbot.state.strictlyOnViewState
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndFSMAndStartLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.command
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import dev.inmo.tgbotapi.utils.RiskFeature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

@OptIn(RiskFeature::class)
suspend fun studentRun(botToken: String, studentIdRegistry: StudentIdRegistry, studentStorage: StudentStorage, core: StudentCore) {
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

    strictlyOnStartState(studentIdRegistry, studentStorage, isDeveloperRun = true)
    strictlyOnMenuState()
    strictlyOnViewState(core)
    strictlyOnSignUpState(core)
    strictlyOnSendSolutionState(core)
    strictlyOnCheckGradesState(core)

    allUpdatesFlow.subscribeSafelyWithoutExceptions(this) {
      println(it)
    }
  }.second.join()
}
