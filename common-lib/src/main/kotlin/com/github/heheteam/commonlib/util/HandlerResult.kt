package com.github.heheteam.commonlib.util

import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.chat.User

sealed interface HandlerResult<out T> {
  data class UserInput<T>(val value: T) : HandlerResult<T>

  data class Action(val action: suspend BehaviourContext.(User, ChatId) -> Unit) :
    HandlerResult<Nothing>
}
