package com.github.heheteam.commonlib.state

import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.util.UpdateHandlersController
import com.github.heheteam.commonlib.util.UserInput
import com.github.heheteam.commonlib.util.delete
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import dev.inmo.micro_utils.coroutines.runCatchingSafely
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage
import dev.inmo.tgbotapi.types.message.textsources.TextSourcesList

sealed interface UserInputParsingResult {
  data class Success(val newState: State) : UserInputParsingResult

  data class Failure(val errorMessage: TextSourcesList) : UserInputParsingResult
}

abstract class TextQueryBotStateWithHandlersAndUserId<Service, UserId> :
  BotStateWithHandlersAndUserId<String, UserInputParsingResult, Service, UserId> {
  abstract val introMessageContent: TextSourcesList

  abstract fun menuState(): State

  override fun defaultState(): State {
    return menuState()
  }

  val sentMessages: MutableList<AccessibleMessage> = mutableListOf()

  abstract fun parseUserTextInput(input: String, service: Service): UserInputParsingResult

  override suspend fun intro(
    bot: BehaviourContext,
    service: Service,
    updateHandlersController: UpdateHandlersController<() -> Unit, String, FrontendError>,
  ): Result<Unit, FrontendError> = coroutineBinding {
    val introMessage = bot.sendMessage(context, introMessageContent)
    sentMessages.add(introMessage)
    updateHandlersController.addTextMessageHandler { textMessage ->
      UserInput(textMessage.content.text)
    }
  }

  override suspend fun computeNewState(
    service: Service,
    input: String,
  ): Result<Pair<State, UserInputParsingResult>, FrontendError> = coroutineBinding {
    val parseUserTextInput = parseUserTextInput(input, service)
    val nextState =
      (parseUserTextInput as? UserInputParsingResult.Success)?.newState
        ?: this@TextQueryBotStateWithHandlersAndUserId
    nextState to parseUserTextInput
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: Service,
    response: UserInputParsingResult,
  ): Result<Unit, FrontendError> = coroutineBinding {
    when (response) {
      is UserInputParsingResult.Failure -> bot.send(context, response.errorMessage)
      is UserInputParsingResult.Success -> Unit
    }
  }

  override suspend fun outro(bot: BehaviourContext, service: Service) {
    for (message in sentMessages) {
      runCatchingSafely { bot.delete(message) }
    }
  }
}
