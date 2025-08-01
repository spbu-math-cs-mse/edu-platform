package com.github.heheteam.studentbot.state

import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import dev.inmo.tgbotapi.extensions.utils.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.textsources.TextSourcesList
import kotlinx.coroutines.flow.first

class ExceptionErrorMessageState(override val context: User, val text: TextSourcesList) : State {
  suspend fun handle(bot: BehaviourContext): State {
    bot.send(context.id, text, replyMarkup = InlineKeyboardMarkup(dataInlineButton("Ок", "ok")))
    bot.waitDataCallbackQuery().first()
    return StartState(context)
  }
}
