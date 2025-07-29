package com.github.heheteam.studentbot

import com.github.heheteam.commonlib.api.ParentApi
import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.util.startStateOnUnhandledUpdate
import com.github.heheteam.studentbot.state.SelectStudentParentState
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.bot.setMyCommands
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndFSMAndStartLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.command
import dev.inmo.tgbotapi.extensions.utils.extensions.parseCommandsWithNamedArgs
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import dev.inmo.tgbotapi.types.BotCommand
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.content.TextMessage
import dev.inmo.tgbotapi.utils.PreviewFeature
import dev.inmo.tgbotapi.utils.RiskFeature
import io.ktor.http.escapeIfNeeded
import java.time.LocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class StudentRunner(
  private val botToken: String,
  private val studentApi: StudentApi,
  private val parentApi: ParentApi,
) {

  @OptIn(RiskFeature::class, PreviewFeature::class)
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

        command("start", requireOnlyCommandInMessage = false) { startFromStartCommand(it) }
        startStateOnUnhandledUpdate { user -> startFromUnhandledUpdate(user) }

        StateRegister(studentApi, parentApi, this).registerStates(botToken)

        allUpdatesFlow.subscribeSafelyWithoutExceptions(this) {
          println(LocalDateTime.now().toString() + " " + it.toString().escapeIfNeeded())
        }
      }
      .second
      .join()

  private suspend fun DefaultBehaviourContextWithFSM<State>.startFromUnhandledUpdate(user: User?) {
    if (user != null) {
      val startingState = SelectStudentParentState(user, null)
      startChain(startingState)
    }
  }

  @OptIn(RiskFeature::class)
  private suspend fun DefaultBehaviourContextWithFSM<State>.startFromStartCommand(
    message: TextMessage
  ) {
    val user = message.from
    if (user != null) {
      val args =
        message.parseCommandsWithNamedArgs(" ")["start"]?.toMap()?.mapKeys { (key, value) ->
          val index = key.indexOf('=')
          if (index == -1) key else key.substring(0, index)
        }
      val from = args?.get("from")
      val courseToken = args?.get("course")
      val state = SelectStudentParentState(user, from, courseToken)
      startChain(state)
    }
  }

  private fun reportExceptionAndPreserveState(state: State, e: Throwable): State {
    println("Thrown error on $state")
    e.printStackTrace()
    val context = state.context
    if (context is User) {
      return SelectStudentParentState(context, from = null)
    }
    return state
  }
}
