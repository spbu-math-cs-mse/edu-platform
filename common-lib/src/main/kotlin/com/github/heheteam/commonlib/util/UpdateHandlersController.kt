package com.github.heheteam.commonlib.util

import com.github.michaelbull.result.Result
import dev.inmo.micro_utils.coroutines.firstNotNull
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

class UpdateHandlersController<ActionT, UserInputT, Err> {
  private val callBackHandlers:
    MutableList<
      suspend (DataCallbackQuery) -> Result<HandlerResultWithUserInput<ActionT, UserInputT>, Err>?
    > =
    mutableListOf()
  private val messageHandlers:
    MutableList<
      (CommonMessage<TextContent>) -> Result<HandlerResultWithUserInput<ActionT, UserInputT>, Err>
    > =
    mutableListOf()

  fun addDataCallbackHandler(
    arg:
      suspend (DataCallbackQuery) -> Result<HandlerResultWithUserInput<ActionT, UserInputT>, Err>?
  ) {
    callBackHandlers.add(arg)
  }

  fun addTextMessageHandler(
    arg:
      (CommonMessage<TextContent>) -> Result<HandlerResultWithUserInput<ActionT, UserInputT>, Err>
  ) {
    messageHandlers.add(arg)
  }

  suspend fun processNextUpdate(
    bot: BehaviourContext,
    chatId: ChatId,
  ): Result<HandlerResultWithUserInput<ActionT, UserInputT>, Err> {
    return merge(
        bot.waitTextMessageWithUser(chatId).map { message ->
          messageHandlers.firstNotNullOfOrNull { handler -> handler.invoke(message) }
        },
        bot.waitDataCallbackQueryWithUser(chatId).map { data ->
          callBackHandlers.firstNotNullOfOrNull { handler -> handler.invoke(data) }
        },
      )
      .firstNotNull()
  }
}
