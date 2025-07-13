package com.github.heheteam.studentbot.state.quiz

import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.studentbot.state.MenuState
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.types.chat.User

class DefaultErrorState(
  override val context: User,
  override val userId: StudentId,
  val nextState: State,
) : QuestState() {
  override suspend fun BotContext.run() {
    val buttons = listOf("✅ Конечно!", "\uD83D\uDD19 Назад")
    send(
        "\uD83D\uDD12 Похоже, этот ответ не совсем верный. Попробуем еще раз?",
        replyMarkup = verticalKeyboard(buttons),
      )
      .also { messagesWithKeyboard.add(it) }
    addDataCallbackHandler { callbackQuery ->
      when (callbackQuery.data) {
        buttons[0] -> NewState(nextState)
        buttons[1] -> NewState(MenuState(context, userId))
        else -> Unhandled
      }
    }
  }
}
