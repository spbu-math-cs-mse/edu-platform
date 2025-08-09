package com.github.heheteam.commonlib.state

import com.github.heheteam.commonlib.AttachmentKind
import com.github.heheteam.commonlib.LocalMediaAttachment
import com.github.heheteam.commonlib.TextWithMediaAttachments
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.util.HandlerResultWithUserInputOrUnhandled
import com.github.heheteam.commonlib.util.MenuKeyboardData
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.UpdateHandlerManager
import com.github.heheteam.commonlib.util.id
import com.github.heheteam.commonlib.util.sendTextWithMediaAttachments
import com.github.michaelbull.result.mapBoth
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.KeyboardMarkup
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.MarkdownParseMode
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.message.textsources.TextSourcesList
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery

typealias HandlersController = UpdateHandlerManager<Unit>

class BotContext(
  private val bot: BehaviourContext,
  private val context: User,
  private val handlersController: HandlersController,
) {

  suspend fun send(text: String, replyMarkup: KeyboardMarkup? = null): ContentMessage<TextContent> =
    bot.send(context, text, replyMarkup = replyMarkup)

  suspend fun sendMarkdown(
    text: String,
    replyMarkup: KeyboardMarkup? = null,
  ): ContentMessage<TextContent> =
    bot.send(context, text, replyMarkup = replyMarkup, parseMode = MarkdownParseMode)

  suspend fun send(
    textContent: TextSourcesList,
    replyMarkup: KeyboardMarkup? = null,
  ): ContentMessage<TextContent> = bot.send(context, textContent, replyMarkup = replyMarkup)

  suspend fun send(
    content: TextWithMediaAttachments,
    replyMarkup: InlineKeyboardMarkup? = null,
  ): ContentMessage<*> =
    bot.sendTextWithMediaAttachments(context.id, content, replyMarkup = replyMarkup).value

  suspend fun sendImage(
    path: String,
    replyMarkup: InlineKeyboardMarkup? = null,
  ): ContentMessage<*> =
    send(
      TextWithMediaAttachments(
        attachments = listOf(LocalMediaAttachment(AttachmentKind.PHOTO, path))
      ),
      replyMarkup,
    )

  fun addDataCallbackHandler(
    arg:
      suspend (DataCallbackQuery) -> HandlerResultWithUserInputOrUnhandled<
          SuspendableBotAction,
          Unit,
          FrontendError,
        >
  ) = handlersController.addDataCallbackHandler(arg)

  fun addTextMessageHandler(
    arg:
      suspend (CommonMessage<TextContent>) -> HandlerResultWithUserInputOrUnhandled<
          SuspendableBotAction,
          Unit,
          FrontendError,
        >
  ) = handlersController.addTextMessageHandler(arg)

  fun registerStateMenu(menu: MenuKeyboardData<NewState>) {
    addDataCallbackHandler {
      menu.handler(it.data).mapBoth(success = ::id, failure = { Unhandled })
    }
  }
}
