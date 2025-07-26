package com.github.heheteam.commonlib.state

import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.errors.TelegramBotError
import com.github.heheteam.commonlib.util.UpdateHandlerManager
import com.github.heheteam.commonlib.util.UserInput
import com.github.heheteam.commonlib.util.sendTextWithMediaAttachments
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.mapError
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User

abstract class QuestionWithTextAnswerBotState<ApiService, UserId>(
  override val context: User,
  override val userId: UserId,
) : QuizBotStateWithHandlersAndUserId<ApiService, UserId>(context, userId) {
  final override suspend fun intro(
    bot: BehaviourContext,
    service: ApiService,
    updateHandlersController: UpdateHandlerManager<String>,
  ): Result<Unit, FrontendError> =
    coroutineBinding {
        bot.sendTextWithMediaAttachments(context.id, question).bind()
        updateHandlersController.addTextMessageHandler { message ->
          UserInput(message.content.text)
        }
      }
      .mapError { TelegramBotError(it) }
}
