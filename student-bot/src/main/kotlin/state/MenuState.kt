package com.github.heheteam.studentbot.state

import com.github.heheteam.studentbot.Dialogues
import com.github.heheteam.studentbot.metaData.ButtonKey
import com.github.heheteam.studentbot.metaData.menuKeyboard
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnMenuState() {
  strictlyOn<MenuState> { state ->
    val stickerMessage = bot.sendSticker(state.context.id, Dialogues.typingSticker)
    val initialMessage =
      bot.send(
        state.context,
        text = Dialogues.menu(),
        replyMarkup = menuKeyboard(),
      )

    val callback = waitDataCallbackQuery().first()
    deleteMessage(initialMessage)
    deleteMessage(stickerMessage)
    when (callback.data) {
      ButtonKey.VIEW -> ViewState(state.context, state.student)
      ButtonKey.SIGN_UP -> SignUpState(state.context, state.student)
      ButtonKey.SEND_SOLUTION -> SendSolutionState(state.context, state.student)
      ButtonKey.CHECK_GRADES -> CheckGradesState(state.context, state.student)
      else -> MenuState(state.context, state.student)
    }
  }
}
