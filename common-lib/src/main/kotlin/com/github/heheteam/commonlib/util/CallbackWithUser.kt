package com.github.heheteam.commonlib.util

import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDocumentMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitMediaMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.DocumentContent
import dev.inmo.tgbotapi.types.message.content.MediaContent
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter

suspend fun BehaviourContext.waitDataCallbackQueryWithUser(
  chatId: ChatId
): Flow<DataCallbackQuery> = waitDataCallbackQuery().filter { it.user.id == chatId }

suspend fun BehaviourContext.waitTextMessageWithUser(
  chatId: ChatId
): Flow<CommonMessage<TextContent>> = waitTextMessage().filter { it.chat.id == chatId }

suspend fun BehaviourContext.waitMediaMessageWithUser(
  chatId: ChatId
): Flow<CommonMessage<MediaContent>> = waitMediaMessage().filter { it.chat.id == chatId }

suspend fun BehaviourContext.waitDocumentMessageWithUser(
  chatId: ChatId
): Flow<CommonMessage<DocumentContent>> = waitDocumentMessage().filter { it.chat.id == chatId }
