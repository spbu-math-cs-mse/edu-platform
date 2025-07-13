package com.github.heheteam.studentbot.state.quiz

import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.studentbot.state.MenuState
import dev.inmo.tgbotapi.types.chat.User

class L2S0(override val context: User, override val userId: StudentId) : QuestState() {
  override suspend fun BotContext.run() {
    send(
      "\uD83C\uDF33 \"Я не дерево. Я ЭНТ! И я задам тебе загадку.\""
    )
    val buttons = listOf("\uD83D\uDE80 Я готов!", "\uD83D\uDD19 Назад\n")
    send(
      "\uD83D\uDC36 Дуся (шёпотом): \"Лучше не спорь… Просто решим задачу и пойдём дальше \uD83D\uDE05\"",
      replyMarkup = horizontalKeyboard(buttons)
    ).also { messagesWithKeyboard.add(it) }
    addDataCallbackHandler { callbackQuery ->
      when (callbackQuery.data) {
        buttons[0] -> NewState(L2S1(context, userId))
        buttons[1] -> NewState(MenuState(context, userId))
        else -> Unhandled
      }
    }
  }
}

class L2S1(override val context: User, override val userId: StudentId) : QuestState() {
  override suspend fun BotContext.run() {
    send("\uD83C\uDF33 \"У меня 3 корня, 5 ветвей и 12 листьев. Сколько у меня всего частей?\"")
    val correctAnswer = 20
    addTextMessageHandler { message ->
      when (message.content.text.trim().toIntOrNull()) {
        null -> {
          send("Надо ввести число")
          NewState(L2S1(context, userId))
        }
        correctAnswer -> NewState(L2S2(context, userId))
        else -> {
          NewState(L2S1Wrong(context, userId))
        }
      }
    }
  }
}

class L2S1Wrong(override val context: User, override val userId: StudentId) : QuestState() {
  override suspend fun BotContext.run() {
    val buttons = listOf("✅ Конечно!", "\uD83D\uDD19 Назад")
    send(
      "Попробуй ещё раз, ты почти у цели!\n",
      replyMarkup = horizontalKeyboard(buttons),
    )
      .also { messagesWithKeyboard.add(it) }
    addDataCallbackHandler { callbackQuery ->
      when (callbackQuery.data) {
        buttons[0] -> NewState(L2S1(context, userId))
        buttons[1] -> NewState(MenuState(context, userId))
        else -> Unhandled
      }
    }
  }
}

class L2S2(override val context: User, override val userId: StudentId) : QuestState() {
  override suspend fun BotContext.run() {
    send(
      "Энт двигает веткой — и из ствола вырастает тропинка, сверкающая математическими символами."
    )
    val buttons = listOf("\uD83D\uDE80 Вперед, в Лабиринт!", "\uD83D\uDD19 Назад\n")
    send(
      "\uD83D\uDC36 Дуся: \"Путь открыт! Пойдём дальше — в Лабиринт Ключей! Там нас ждёт настоящее испытание\"",
      replyMarkup = verticalKeyboard(buttons)
    ).also { messagesWithKeyboard.add(it) }
    addDataCallbackHandler { callbackQuery ->
      when (callbackQuery.data) {
        buttons[0] -> NewState(L3S1(context, userId))
        buttons[1] -> NewState(MenuState(context, userId))
        else -> Unhandled
      }
    }
  }
}

