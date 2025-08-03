package com.github.heheteam.studentbot.state.quiz

import com.github.heheteam.commonlib.api.CommonUserApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.errors.toTelegramError
import com.github.heheteam.commonlib.interfaces.CommonUserId
import com.github.heheteam.commonlib.interfaces.ParentId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.state.BotStateWithHandlersAndUserId
import com.github.heheteam.commonlib.util.UpdateHandlerManager
import com.github.heheteam.commonlib.util.ok
import com.github.heheteam.studentbot.state.MenuState
import com.github.heheteam.studentbot.state.parent.ParentMenuState
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.runCatching
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.warning
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.bot.exceptions.CommonRequestException
import dev.inmo.tgbotapi.extensions.api.edit.reply_markup.editMessageReplyMarkup
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.utils.row
import kotlin.reflect.full.primaryConstructor

fun horizontalKeyboard(buttons: List<String>) = inlineKeyboard {
  row { buttons.map { dataButton(it, it) } }
}

fun verticalKeyboard(buttons: List<String>) = inlineKeyboard {
  buttons.map { row { dataButton(it, it) } }
}

abstract class QuestState<ApiService : CommonUserApi<UserId>, UserId : CommonUserId> :
  BotStateWithHandlersAndUserId<String, Unit, ApiService, UserId> {

  var messagesWithKeyboard: MutableList<ContentMessage<*>> = mutableListOf()

  override suspend fun outro(bot: BehaviourContext, service: ApiService) {
    messagesWithKeyboard.forEach { messageWithKeyboard ->
      try {
        bot.editMessageReplyMarkup(messageWithKeyboard, replyMarkup = null)
      } catch (e: CommonRequestException) {
        KSLog.warning("Failed to delete inline keyboard", e)
      }
    }
  }

  abstract suspend fun QuestBotContext.run(service: ApiService)

  override suspend fun intro(
    bot: BehaviourContext,
    service: ApiService,
    updateHandlersController: UpdateHandlerManager<String>,
  ): Result<Unit, FrontendError> =
    runCatching { QuestBotContext(bot, context, updateHandlersController).run(service) }
      .toTelegramError()

  override suspend fun computeNewState(
    service: ApiService,
    input: String,
  ): Result<Pair<State, Unit>, FrontendError> = (defaultState() to Unit).ok()

  override fun defaultState(): State = menuState()

  fun menuState(): State =
    when (userId as CommonUserId) {
      is StudentId -> MenuState(context, userId as StudentId)
      is ParentId -> ParentMenuState(context, userId as ParentId)
    }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: ApiService,
    response: Unit,
    input: String,
  ): Result<Unit, FrontendError> = Unit.ok()

  fun saveState(service: ApiService) {
    service.saveCurrentQuestState(userId, this.javaClass.name)
  }

  companion object {
    fun <ApiService : CommonUserApi<UserId>, UserId : CommonUserId> restoreState(
      className: String?,
      context: User,
      userId: UserId,
    ): Result<State, FrontendError> =
      runCatching {
          if (className == null) {
            return@runCatching when (userId) {
              is StudentId -> L0Student(context, userId)
              is ParentId -> L0Parent(context, userId)
              else -> error("unreahcalbe")
            }
          }
          if (!className.contains("Student") && !className.contains("Parent")) {
            return@runCatching when (userId) {
              is StudentId -> L0Student(context, userId)
              is ParentId -> L0Parent(context, userId)
              else -> error("unreahcalbe")
            }
          }
          val constructor = Class.forName(className).kotlin.primaryConstructor
          constructor?.call(context, userId) as QuestState<ApiService, UserId>
        }
        .toTelegramError()
  }
}
