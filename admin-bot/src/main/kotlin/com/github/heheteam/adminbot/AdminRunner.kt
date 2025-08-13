package com.github.heheteam.adminbot

import com.github.heheteam.adminbot.states.StartState
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.util.getCurrentMoscowTime
import com.github.heheteam.commonlib.util.startStateOnUnhandledUpdate
import com.github.michaelbull.result.get
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.bot.setMyCommands
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndFSMAndStartLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.command
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import dev.inmo.tgbotapi.extensions.utils.groupChatOrNull
import dev.inmo.tgbotapi.types.BotCommand
import dev.inmo.tgbotapi.types.message.content.TextMessage
import dev.inmo.tgbotapi.utils.RiskFeature
import dev.inmo.tgbotapi.utils.buildEntities
import dev.inmo.tgbotapi.utils.code
import io.ktor.http.escapeIfNeeded
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

internal const val DEFAULT_ADMIN_ID = 0L

class AdminRunner(private val adminApi: AdminApi) {

  @OptIn(RiskFeature::class)
  suspend fun run(botToken: String) {
    telegramBotWithBehaviourAndFSMAndStartLongPolling(
        botToken,
        CoroutineScope(Dispatchers.IO),
        onStateHandlingErrorHandler = { state, e ->
          println("Thrown error in AdminBot on $state")
          e.printStackTrace()
          state
        },
      ) {
        println(getMe())

        setMyCommands(
          listOf(BotCommand("start", "Start bot"), BotCommand("menu", "Resend menu message"))
        )
        command("start") {
          val user = it.from
          if (user != null) startChain(StartState(user))
        }
        command("bind") { tryBindingChatToErrorService(it) }

        startStateOnUnhandledUpdate { user -> if (user != null) startChain(StartState(user)) }

        StateRegister(adminApi, this).registerStates(botToken)

        allUpdatesFlow.subscribeSafelyWithoutExceptions(this) {
          println(getCurrentMoscowTime().toString() + " " + it.toString().escapeIfNeeded())
        }
      }
      .second
      .join()
  }

  @OptIn(RiskFeature::class)
  private suspend fun DefaultBehaviourContextWithFSM<State>.tryBindingChatToErrorService(
    message: TextMessage
  ) {
    val chat = message.chat.groupChatOrNull() ?: return
    val user = message.from
    if (user != null && adminApi.tgIdIsInWhitelist(user.id).get() ?: false) {
      adminApi.bindErrorChat(chat.id.chatId)
      bot.send(chat, "This chat has been successfully bound!")
    } else {
      val text = buildEntities {
        +"Your Telegram ID ("
        code(user?.id.toString())
        +") is not in the whitelist."
      }
      bot.send(chat, text)
    }
  }
}
