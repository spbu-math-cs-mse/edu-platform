package com.github.heheteam.commonlib.util

import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage

suspend fun <R> BehaviourContext.withMessageCleanup(
  message: ContentMessage<*>,
  f: suspend BehaviourContext.() -> R,
): R {
  val result = f()
  delete(message)
  return result
}

suspend fun BehaviourContext.delete(vararg messages: AccessibleMessage) {
  for (message in messages) {
    delete(message)
  }
}
