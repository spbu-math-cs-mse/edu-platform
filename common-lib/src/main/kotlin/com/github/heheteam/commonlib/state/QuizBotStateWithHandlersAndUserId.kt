package com.github.heheteam.commonlib.state

import com.github.heheteam.commonlib.TextWithMediaAttachments
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.errors.toTelegramError
import com.github.heheteam.commonlib.util.ok
import com.github.heheteam.commonlib.util.sendTextWithMediaAttachments
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.runCatching
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User

abstract class QuizBotStateWithHandlersAndUserId<ApiService, UserId>(
  override val context: User,
  override val userId: UserId,
) : BotStateWithHandlersAndUserId<String, Boolean, ApiService, UserId> {
  abstract val question: TextWithMediaAttachments
  abstract val correctAnswer: String
  abstract val correctAnswerReply: TextWithMediaAttachments
  abstract val incorrectAnswerReply: TextWithMediaAttachments
  abstract val nextState: (User, UserId) -> State
  abstract val menuState: (User, UserId) -> State

  abstract fun checkAnswer(answer: String): Boolean

  override suspend fun outro(bot: BehaviourContext, service: ApiService) = Unit

  override suspend fun computeNewState(
    service: ApiService,
    input: String,
  ): Result<Pair<State, Boolean>, FrontendError> =
    if (checkAnswer(input)) {
        nextState(context, userId) to true
      } else {
        menuState(context, userId) to false
      }
      .ok()

  override fun defaultState(): State = menuState(context, userId)

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: ApiService,
    response: Boolean,
    input: String,
  ): Result<Unit, FrontendError> =
    runCatching {
        if (response) {
          bot.sendTextWithMediaAttachments(context.id, correctAnswerReply)
        } else {
          bot.sendTextWithMediaAttachments(context.id, incorrectAnswerReply)
        }
        Unit
      }
      .toTelegramError()
}
