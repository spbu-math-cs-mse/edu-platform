package com.github.heheteam.commonlib.state

import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.heheteam.commonlib.util.MenuKeyboardData
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.UpdateHandlersController
import com.github.heheteam.commonlib.util.UserInput
import com.github.heheteam.commonlib.util.delete
import com.github.michaelbull.result.mapBoth
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage
import dev.inmo.tgbotapi.types.message.textsources.TextSourcesList

abstract class NavigationBotStateWithHandlersAndUserId<Service, UserId> :
  BotStateWithHandlersAndUserId<State?, Unit, Service, UserId> {
  abstract val introMessageContent: TextSourcesList

  abstract fun createKeyboard(service: Service): MenuKeyboardData<State?>

  abstract fun menuState(): State

  val sentMessages: MutableList<AccessibleMessage> = mutableListOf()

  override suspend fun intro(
    bot: BehaviourContext,
    service: Service,
    updateHandlersController: UpdateHandlersController<() -> Unit, State?, Any>,
  ) {
    val keyboardData = createKeyboard(service)
    val introMessage =
      bot.sendMessage(context, introMessageContent, replyMarkup = keyboardData.keyboard)
    sentMessages.add(introMessage)
    updateHandlersController.addDataCallbackHandler { dataCallbackQuery ->
      keyboardData
        .handler(dataCallbackQuery.data)
        .mapBoth(success = { UserInput(it) }, failure = { Unhandled })
    }
  }

  override fun computeNewState(service: Service, input: State?): Pair<State, Unit> =
    if (input != null) input to Unit else menuState() to Unit

  override suspend fun sendResponse(bot: BehaviourContext, service: Service, response: Unit) = Unit

  override suspend fun outro(bot: BehaviourContext, service: Service) {
    for (message in sentMessages) {
      bot.delete(message)
    }
  }
}

abstract class NavigationBotStateWithHandlersAndStudentId<Service> :
  NavigationBotStateWithHandlersAndUserId<Service, StudentId>()

abstract class NavigationBotStateWithHandlersAndTeacherId<Service> :
  NavigationBotStateWithHandlersAndUserId<Service, TeacherId>()
