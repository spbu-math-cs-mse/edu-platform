package com.github.heheteam.studentbot.state.quiz

import com.github.heheteam.commonlib.AttachmentKind
import com.github.heheteam.commonlib.LocalMediaAttachment
import com.github.heheteam.commonlib.TextWithMediaAttachments
import com.github.heheteam.commonlib.api.CommonUserApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.interfaces.CommonUserId
import com.github.heheteam.commonlib.util.HandlerResultWithUserInputOrUnhandled
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.UpdateHandlersController
import com.github.heheteam.commonlib.util.sendTextWithMediaAttachments
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

typealias HandlersController = UpdateHandlersController<() -> Unit, String, FrontendError>

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
          () -> Unit,
          String,
          FrontendError,
        >
  ) = handlersController.addDataCallbackHandler(arg)

  fun addTextMessageHandler(
    arg:
      suspend (CommonMessage<TextContent>) -> HandlerResultWithUserInputOrUnhandled<
          () -> Unit,
          String,
          FrontendError,
        >
  ) = handlersController.addTextMessageHandler(arg)

  fun <ApiService : CommonUserApi<UserId>, UserId : CommonUserId> addIntegerReadHandler(
    trueAnswer: Int,
    thisState: QuestState<ApiService, UserId>,
    actionOnCorrectAnswer: (suspend BotContext.() -> QuestState<ApiService, UserId>),
    actionOnWrongAnswer: (suspend BotContext.() -> QuestState<ApiService, UserId>),
  ) {
    addTextMessageHandler { message ->
      when (message.content.text.trim().toIntOrNull()) {
        null -> {
          send("Надо ввести число")
          NewState(thisState)
        }

        trueAnswer -> NewState(actionOnCorrectAnswer.invoke(this))

        else -> NewState(actionOnWrongAnswer.invoke(this))
      }
    }
  }

  fun <ApiService : CommonUserApi<UserId>, UserId : CommonUserId> addStringReadHandler(
    trueAnswer: String,
    actionOnCorrectAnswer: (suspend BotContext.() -> QuestState<ApiService, UserId>),
    actionOnWrongAnswer: (suspend BotContext.() -> QuestState<ApiService, UserId>),
  ) {
    addTextMessageHandler { message ->
      when (message.content.text.trim()) {
        trueAnswer -> NewState(actionOnCorrectAnswer.invoke(this))
        else -> {
          NewState(actionOnWrongAnswer.invoke(this))
        }
      }
    }
  }
}
