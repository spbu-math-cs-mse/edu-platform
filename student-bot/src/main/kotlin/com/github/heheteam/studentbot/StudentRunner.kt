package com.github.heheteam.studentbot

import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.studentbot.state.StartState
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.bot.setMyCommands
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndFSMAndStartLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.command
import dev.inmo.tgbotapi.extensions.utils.extensions.parseCommandsWithArgs
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import dev.inmo.tgbotapi.types.BotCommand
import dev.inmo.tgbotapi.utils.RiskFeature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class StudentRunner(private val botToken: String, private val studentApi: StudentApi) {

  @OptIn(RiskFeature::class)
  suspend fun run() =
    telegramBotWithBehaviourAndFSMAndStartLongPolling(
        botToken,
        CoroutineScope(Dispatchers.IO),
        onStateHandlingErrorHandler = ::reportExceptionAndPreserveState,
      ) {
        println(getMe())

        setMyCommands(
          listOf(BotCommand("start", "Start bot"), BotCommand("menu", "Resend menu message"))
        )

        command("start", requireOnlyCommandInMessage = false) {
          val user = it.from
          if (user != null) {
            val token = it.parseCommandsWithArgs(" ")["start"]?.firstOrNull()
            val startingState = StartState(user, token)
            startChain(startingState)
          }
        }

        StateRegister(studentApi, this).registerStates(botToken)

        allUpdatesFlow.subscribeSafelyWithoutExceptions(this) { println(it) }
      }
      .second
      .join()

  private fun reportExceptionAndPreserveState(state: State, e: Throwable): State {
    println("Thrown error on $state")
    e.printStackTrace()
    return state
  }
}
