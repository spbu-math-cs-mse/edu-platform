package com.github.heheteam.commonlib.util

import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDocumentMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitMediaMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.message
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.DocumentContent
import dev.inmo.tgbotapi.types.message.content.MediaContent
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.types.toChatId
import dev.inmo.tgbotapi.utils.RiskFeature
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter

@OptIn(RiskFeature::class)
suspend fun BehaviourContext.waitDataCallbackQueryWithUser(
  chatId: ChatId
): Flow<DataCallbackQuery> =
  waitDataCallbackQuery().filter { it.message?.chat?.id?.chatId?.toChatId() == chatId }

suspend fun BehaviourContext.waitTextMessageWithUser(
  chatId: ChatId
): Flow<CommonMessage<TextContent>> = waitTextMessage().filter { it.chat.id == chatId }

suspend fun BehaviourContext.waitMediaMessageWithUser(
  chatId: ChatId
): Flow<CommonMessage<MediaContent>> = waitMediaMessage().filter { it.chat.id == chatId }

suspend fun BehaviourContext.waitDocumentMessageWithUser(
  chatId: ChatId
): Flow<CommonMessage<DocumentContent>> = waitDocumentMessage().filter { it.chat.id == chatId }
