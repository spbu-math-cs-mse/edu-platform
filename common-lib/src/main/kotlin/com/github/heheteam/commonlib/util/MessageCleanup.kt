package com.github.heheteam.commonlib.util

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.warning
import dev.inmo.tgbotapi.bot.exceptions.CommonRequestException
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage

suspend fun <R> BehaviourContext.withMessageCleanup(
  message: ContentMessage<*>,
  f: suspend BehaviourContext.() -> R,
): R {
  val result = f()
  try {
    delete(message)
  } catch (e: CommonRequestException) {
    KSLog.warning("Failed to delete message", e)
  }
  return result
}

suspend fun BehaviourContext.delete(vararg messages: AccessibleMessage) {
  for (message in messages) {
    try {
      delete(message)
    } catch (e: CommonRequestException) {
      KSLog.warning("Failed to delete message", e)
    }
  }
}
