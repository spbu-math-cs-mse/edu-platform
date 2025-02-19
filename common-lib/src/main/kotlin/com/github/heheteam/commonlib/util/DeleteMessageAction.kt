package com.github.heheteam.commonlib.util

import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.MessageId
import kotlinx.serialization.Serializable

@Serializable data class DeleteMessageAction(val chatId: ChatId, val messageId: MessageId)
