package com.github.heheteam.studentbot.state.parent

import com.github.heheteam.commonlib.api.ParentApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.interfaces.ParentId
import com.github.heheteam.commonlib.state.BotStateWithHandlersAndParentId
import com.github.heheteam.commonlib.util.ButtonData
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
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery

class ParentStartQuestState(override val context: User, override val userId: ParentId) :
  BotStateWithHandlersAndParentId<Boolean, Unit, ParentApi> {
  private lateinit var confirmMessage: AccessibleMessage

  override fun defaultState(): State = ParentMenuState(context, userId)

  override suspend fun intro(
    bot: BehaviourContext,
    service: ParentApi,
    updateHandlersController: UpdateHandlersController<() -> Unit, Boolean, FrontendError>,
  ): Result<Unit, FrontendError> = coroutineBinding {
    val confirmMessageKeyboard =
      buildColumnMenu(
        ButtonData("\uD83D\uDC49 Начать игру (от лица ребёнка)", "yes") { true },
        ButtonData("\uD83D\uDD19 Назад", "no") { false },
      )
    confirmMessage =
      bot.sendMessage(
        context,
        "Если ваш ребёнок рядом — передайте ему телефон, и мы начнём игру!\n" +
          "Если хотите посмотреть самостоятельно, нажмите кнопку ниже.",
        replyMarkup = confirmMessageKeyboard.keyboard,
      )
    updateHandlersController.addDataCallbackHandler { value: DataCallbackQuery ->
      val result = confirmMessageKeyboard.handler.invoke(value.data)
      result.mapBoth(success = { UserInput(it) }, failure = { Unhandled })
    }
  }

  override suspend fun computeNewState(
    service: ParentApi,
    input: Boolean,
  ): Result<Pair<State, Unit>, FrontendError> =
    ((if (input) ParentMenuState(context, userId) else ParentMenuState(context, userId)) to Unit)
      .ok()

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: ParentApi,
    response: Unit,
  ): Result<Unit, FrontendError> = Unit.ok()

  override suspend fun outro(bot: BehaviourContext, service: ParentApi) = Unit
}
