package com.github.heheteam.commonlib.state

import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.errors.TelegramBotError
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.UpdateHandlerManager
import com.github.heheteam.commonlib.util.UserInput
import com.github.heheteam.commonlib.util.sendTextWithMediaAttachments
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.mapError
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.utils.row

abstract class MultipleChoiceQuestionBotState<ApiService, UserId>(
  override val context: User,
  override val userId: UserId,
) : QuizBotStateWithHandlersAndUserId<ApiService, UserId>(context, userId) {
  abstract val incorrectAnswers: List<String>
  private val answers: List<String> by lazy { incorrectAnswers + correctAnswer }

  final override fun checkAnswer(answer: String): Boolean = answer == correctAnswer

  final override suspend fun intro(
    bot: BehaviourContext,
    service: ApiService,
    updateHandlersController: UpdateHandlerManager<String>,
  ): Result<Unit, FrontendError> =
    coroutineBinding {
        bot
          .sendTextWithMediaAttachments(
            context.id,
            question,
            replyMarkup =
              inlineKeyboard { answers.shuffled().forEach { row { dataButton(it, it) } } },
          )
          .bind()

        updateHandlersController.addDataCallbackHandler { callback ->
          if (answers.contains(callback.data)) {
            UserInput(callback.data)
          } else {
            Unhandled
          }
        }
      }
      .mapError { TelegramBotError(it) }
}
