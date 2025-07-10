package com.github.heheteam.commonlib.util

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.warning
import dev.inmo.tgbotapi.bot.exceptions.CommonRequestException
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onContentMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onMessageCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onUnhandledCommand
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.message
import dev.inmo.tgbotapi.extensions.utils.groupChatOrNull
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.utils.PreviewFeature
import dev.inmo.tgbotapi.utils.RiskFeature

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

@OptIn(RiskFeature::class, PreviewFeature::class)
suspend fun BehaviourContext.startStateOnUnhandledUpdate(handleAction: suspend (User?) -> Unit) {
  onUnhandledCommand { if (it.chat.groupChatOrNull() == null) handleAction(it.from) }
  onMessageCallbackQuery { if (it.message.chat.groupChatOrNull() == null) handleAction(it.from) }
  onDataCallbackQuery { if (it.message?.chat?.groupChatOrNull() == null) handleAction(it.from) }
  onContentMessage { if (it.chat.groupChatOrNull() == null) handleAction(it.from) }
}
