package com.github.heheteam.commonlib.util

import com.github.michaelbull.result.Result
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery

sealed interface HandlerResultWithUserInput<out ActionT, out UserInputT>

sealed interface HandlerResult<out ActionT>

data class ActionWrapper<ActionT>(val action: ActionT) :
  HandlerResult<ActionT>, HandlerResultWithUserInput<ActionT, Nothing>

data class NewState(val state: State) :
  HandlerResult<Nothing>, HandlerResultWithUserInput<Nothing, Nothing>

data class UserInput<UserInputT>(val input: UserInputT) :
  HandlerResultWithUserInput<Nothing, UserInputT>

// null implies that the update was probably not for this handler;
// result Err implies that the update should be handled by this handler, but failed
typealias DataCallbackHandler<T, Err> = (DataCallbackQuery) -> Result<HandlerResult<T>, Err>?

typealias TextMessageHandler<T, Err> =
  (CommonMessage<TextContent>) -> Result<HandlerResult<T>, Err>?
