package com.github.heheteam.studentbot.state.quiz

import com.github.heheteam.commonlib.AttachmentKind
import com.github.heheteam.commonlib.LocalMediaAttachment
import com.github.heheteam.commonlib.TextWithMediaAttachments
import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.errors.toTelegramError
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.state.BotStateWithHandlersAndUserId
import com.github.heheteam.commonlib.util.HandlerResultWithUserInputOrUnhandled
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.UpdateHandlersController
import com.github.heheteam.commonlib.util.ok
import com.github.heheteam.commonlib.util.sendTextWithMediaAttachments
import com.github.heheteam.studentbot.state.MenuState
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.runCatching
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.warning
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.bot.exceptions.CommonRequestException
import dev.inmo.tgbotapi.extensions.api.edit.reply_markup.editMessageReplyMarkup
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.KeyboardMarkup
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.MarkdownParseMode
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.message.textsources.TextSourcesList
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.row

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

  fun addIntegerReadHandler(
    trueAnswer: Int,
    thisState: QuestState,
    actionOnCorrectAnswer: (suspend BotContext.() -> QuestState),
    actionOnWrongAnswer: (suspend BotContext.() -> QuestState),
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

  fun addStringReadHandler(
    trueAnswer: String,
    actionOnCorrectAnswer: (suspend BotContext.() -> QuestState),
    actionOnWrongAnswer: (suspend BotContext.() -> QuestState),
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

fun horizontalKeyboard(buttons: List<String>) = inlineKeyboard {
  row { buttons.map { dataButton(it, it) } }
}

fun verticalKeyboard(buttons: List<String>) = inlineKeyboard {
  buttons.map { row { dataButton(it, it) } }
}

abstract class QuestState : BotStateWithHandlersAndUserId<String, Unit, StudentApi, StudentId> {

  var messagesWithKeyboard: MutableList<ContentMessage<*>> = mutableListOf()

  override suspend fun outro(bot: BehaviourContext, service: StudentApi) {
    messagesWithKeyboard.forEach { messageWithKeyboard ->
      try {
        bot.editMessageReplyMarkup(messageWithKeyboard, replyMarkup = null)
      } catch (e: CommonRequestException) {
        KSLog.warning("Failed to delete inline keyboard", e)
      }
    }
  }

  abstract suspend fun BotContext.run()

  override suspend fun intro(
    bot: BehaviourContext,
    service: StudentApi,
    updateHandlersController: UpdateHandlersController<() -> Unit, String, FrontendError>,
  ): Result<Unit, FrontendError> =
    runCatching { BotContext(bot, context, updateHandlersController).run() }.toTelegramError()

  override suspend fun computeNewState(
    service: StudentApi,
    input: String,
  ): Result<Pair<State, Unit>, FrontendError> = (defaultState() to Unit).ok()

  override fun defaultState(): State = MenuState(context, userId)

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: StudentApi,
    response: Unit,
  ): Result<Unit, FrontendError> = Unit.ok()
}
