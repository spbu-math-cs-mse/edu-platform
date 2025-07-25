package com.github.heheteam.studentbot.state.parent

import com.github.heheteam.commonlib.AttachmentKind
import com.github.heheteam.commonlib.LocalMediaAttachment
import com.github.heheteam.commonlib.api.ParentApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.errors.toTelegramError
import com.github.heheteam.commonlib.interfaces.ParentId
import com.github.heheteam.commonlib.state.BotStateWithHandlersAndParentId
import com.github.heheteam.commonlib.state.UpdateHandlersControllerDefault
import com.github.heheteam.commonlib.util.HandlerResultWithUserInputOrUnhandled
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.UserInput
import com.github.heheteam.commonlib.util.delete
import com.github.heheteam.commonlib.util.ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.runCatching
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.media.sendVideoNote
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.requests.abstracts.asMultipartFile
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery

class ParentAboutKamenetskiState(override val context: User, override val userId: ParentId) :
  BotStateWithHandlersAndParentId<State, Unit, ParentApi> {
  private val sentMessages = mutableListOf<AccessibleMessage>()

  override fun defaultState(): State = ParentMenuState(context, userId)

  override suspend fun intro(
    bot: BehaviourContext,
    service: ParentApi,
    updateHandlersController: UpdateHandlersControllerDefault<State>,
  ): Result<Unit, FrontendError> = coroutineBinding {
    val file = LocalMediaAttachment(AttachmentKind.PHOTO, "/kamen.mp4").openFile()
    bot.sendVideoNote(context, file.asMultipartFile())
    bot.sendMessage(
      context,
      "\uD83D\uDC36 Дуся:" +
        "\"Можете теперь посмотреть видео от Дмитрия или перейти узнать подробнее о курсе.\"",
      replyMarkup = ParentKeyboards.defaultKeyboard(includeMax = true),
    )
    updateHandlersController.addDataCallbackHandler(::processKeyboardButtonPresses)
  }

  private fun processKeyboardButtonPresses(
    callback: DataCallbackQuery
  ): HandlerResultWithUserInputOrUnhandled<Nothing, State, Nothing> {
    val state =
      when (callback.data) {
        ParentKeyboards.RETURN_BACK -> ParentAboutCourseState(context, userId)
        ParentKeyboards.KAMEN -> ParentAboutKamenetskiState(context, userId)
        ParentKeyboards.MAXIMOV -> ParentAboutMaximovState(context, userId)
        else -> null
      }
    return if (state != null) {
      UserInput(state)
    } else {
      Unhandled
    }
  }

  override suspend fun computeNewState(
    service: ParentApi,
    input: State,
  ): Result<Pair<State, Unit>, FrontendError> {
    return Pair(input, Unit).ok()
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: ParentApi,
    response: Unit,
    input: State,
  ): Result<Unit, FrontendError> =
    runCatching { sentMessages.forEach { message -> bot.delete(message) } }.toTelegramError()

  override suspend fun outro(bot: BehaviourContext, service: ParentApi) = Unit
}
