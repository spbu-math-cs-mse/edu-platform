package com.github.heheteam.studentbot

import com.github.heheteam.commonlib.api.ParentApi
import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.errors.ErrorManagementService
import com.github.heheteam.commonlib.errors.UncaughtExceptionError
import com.github.heheteam.commonlib.util.getCurrentMoscowTime
import com.github.heheteam.commonlib.util.startStateOnUnhandledUpdate
import com.github.heheteam.studentbot.state.ExceptionErrorMessageState
import com.github.heheteam.studentbot.state.StartState
import dev.inmo.kslog.common.error
import dev.inmo.kslog.common.logger
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
import dev.inmo.tgbotapi.utils.buildEntities
import io.ktor.http.escapeIfNeeded
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class StudentRunner(
  private val botToken: String,
  private val studentApi: StudentApi,
  private val parentApi: ParentApi,
  private val errorManagementService: ErrorManagementService,
) {

  @OptIn(RiskFeature::class, PreviewFeature::class)
  suspend fun run() =
    telegramBotWithBehaviourAndFSMAndStartLongPolling(
        botToken,
        CoroutineScope(Dispatchers.IO),
        onStateHandlingErrorHandler = ::reportExceptionAndGoToStartingState,
      ) {
        println(getMe())

        setMyCommands(
          listOf(BotCommand("start", "Start bot"), BotCommand("menu", "Resend menu message"))
        )

        command("start", requireOnlyCommandInMessage = false) { startFromStartCommand(it) }
        startStateOnUnhandledUpdate { user -> startFromUnhandledUpdate(user) }

        StateRegister(studentApi, parentApi, this).registerStates(botToken)

        allUpdatesFlow.subscribeSafelyWithoutExceptions(this) {
          println(getCurrentMoscowTime().toString() + " " + it.toString().escapeIfNeeded())
        }
      }
      .second
      .join()

  private suspend fun DefaultBehaviourContextWithFSM<State>.startFromUnhandledUpdate(user: User?) {
    if (user != null) {
      val startingState = StartState(user)
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
      val state = StartState(user, from, courseToken)
      startChain(state)
    }
  }

  private fun reportExceptionAndGoToStartingState(state: State, e: Throwable): State {
    println("Thrown error on $state")
    e.printStackTrace()
    val context = state.context
    if (context is User) {
      val error = errorManagementService.registerError(UncaughtExceptionError(e))
      return ExceptionErrorMessageState(
        context,
        buildEntities {
          +"Случилась ошибка! Не волнуйтесь, разработчики уже в пути ее решения!\n"
          +"Ошибка #${error.number}"
        },
      )
    } else {
      logger.error("context is not User in state $state")
      return state
    }
  }
}
