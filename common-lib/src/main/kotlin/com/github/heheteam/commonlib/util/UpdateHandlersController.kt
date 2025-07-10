package com.github.heheteam.commonlib.util

import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.state.SuspendableBotAction
import dev.inmo.micro_utils.coroutines.firstNotNull
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.DocumentContent
import dev.inmo.tgbotapi.types.message.content.MediaContent
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

class UpdateHandlersController<ActionT, UserInputT, Err> {
  private val callBackHandlers:
    MutableList<
      suspend (DataCallbackQuery) -> HandlerResultWithUserInputOrUnhandled<ActionT, UserInputT, Err>
    > =
    mutableListOf()
  private val textMessageHandlers:
    MutableList<
      suspend (CommonMessage<TextContent>) -> HandlerResultWithUserInputOrUnhandled<
          ActionT,
          UserInputT,
          Err,
        >
    > =
    mutableListOf()

  private val mediaMessagesHandler:
    MutableList<
      suspend (CommonMessage<MediaContent>) -> HandlerResultWithUserInputOrUnhandled<
          ActionT,
          UserInputT,
          Err,
        >
    > =
    mutableListOf()

  private val documentMessagesHandler:
    MutableList<
      suspend (CommonMessage<DocumentContent>) -> HandlerResultWithUserInputOrUnhandled<
          ActionT,
          UserInputT,
          Err,
        >
    > =
    mutableListOf()

  fun addDataCallbackHandler(
    arg:
      suspend (DataCallbackQuery) -> HandlerResultWithUserInputOrUnhandled<ActionT, UserInputT, Err>
  ) {
    callBackHandlers.add(arg)
  }

  fun addTextMessageHandler(
    arg:
      suspend (CommonMessage<TextContent>) -> HandlerResultWithUserInputOrUnhandled<
          ActionT,
          UserInputT,
          Err,
        >
  ) {
    textMessageHandlers.add(arg)
  }

  fun addMediaMessageHandler(
    arg:
      suspend (CommonMessage<MediaContent>) -> HandlerResultWithUserInputOrUnhandled<
          ActionT,
          UserInputT,
          Err,
        >
  ) {
    mediaMessagesHandler.add(arg)
  }

  fun addDocumentMessageHandler(
    arg:
      suspend (CommonMessage<MediaContent>) -> HandlerResultWithUserInputOrUnhandled<
          ActionT,
          UserInputT,
          Err,
        >
  ) {
    mediaMessagesHandler.add(arg)
  }

  suspend fun processNextUpdate(
    bot: BehaviourContext,
    chatId: ChatId,
  ): HandlerResultWithUserInput<ActionT, UserInputT, Err> {
    return merge(
        bot.waitTextMessageWithUser(chatId).map { message ->
          textMessageHandlers
            .map { handler -> handler.invoke(message) }
            .filterIsInstance<HandlerResultWithUserInput<ActionT, UserInputT, Err>>()
            .firstOrNull()
        },
        bot.waitDataCallbackQueryWithUser(chatId).map { data ->
          callBackHandlers
            .map { handler -> handler.invoke(data) }
            .filterIsInstance<HandlerResultWithUserInput<ActionT, UserInputT, Err>>()
            .firstOrNull()
        },
        bot.waitMediaMessageWithUser(chatId).map { data ->
          mediaMessagesHandler
            .map { handler -> handler.invoke(data) }
            .filterIsInstance<HandlerResultWithUserInput<ActionT, UserInputT, Err>>()
            .firstOrNull()
        },
        bot.waitDocumentMessageWithUser(chatId).map { data ->
          documentMessagesHandler
            .map { handler -> handler.invoke(data) }
            .filterIsInstance<HandlerResultWithUserInput<ActionT, UserInputT, Err>>()
            .firstOrNull()
        },
      )
      .firstNotNull()
  }
}

typealias UpdateHandlerManager<In> =
  UpdateHandlersController<SuspendableBotAction, In, FrontendError>
