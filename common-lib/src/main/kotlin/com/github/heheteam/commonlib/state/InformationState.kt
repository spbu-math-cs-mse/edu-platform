package com.github.heheteam.commonlib.state

import com.github.heheteam.commonlib.TextWithMediaAttachments
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.util.ButtonData
import com.github.heheteam.commonlib.util.MenuKeyboardData
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.UpdateHandlersController
import com.github.heheteam.commonlib.util.UserInput
import com.github.heheteam.commonlib.util.buildColumnMenu
import com.github.heheteam.commonlib.util.ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.mapBoth
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.textsources.TextSourcesList
import dev.inmo.tgbotapi.utils.buildEntities

class InformationState<Service, UserId>(
  override val context: User,
  override val userId: UserId,
  val contentGenerator: () -> Result<TextWithMediaAttachments, FrontendError>,
  val nextState: State,
) : NavigationBotStateWithHandlersAndUserId<Service, UserId>() {
  override val introMessageContent: TextSourcesList
    get() = buildEntities { +"Unused" }

  override suspend fun intro(
    bot: BehaviourContext,
    service: Service,
    updateHandlersController: UpdateHandlersController<() -> Unit, State?, FrontendError>,
  ): Result<Unit, FrontendError> = coroutineBinding {
    val introMessageContent = contentGenerator().bind()
    val keyboardData = createKeyboard(service).bind()
    val introMessage =
      bot.sendMessage(context, introMessageContent.text, replyMarkup = keyboardData.keyboard)
    sentMessages.add(introMessage)
    updateHandlersController.addDataCallbackHandler { dataCallbackQuery ->
      keyboardData
        .handler(dataCallbackQuery.data)
        .mapBoth(success = { UserInput(it) }, failure = { Unhandled })
    }
  }

  override fun createKeyboard(service: Service): Result<MenuKeyboardData<State?>, FrontendError> =
    buildColumnMenu(ButtonData<State?>("Ок", "ok") { nextState }).ok()

  override fun menuState(): State = nextState
}
