package com.github.heheteam.studentbot.state.quiz

import com.github.heheteam.commonlib.AttachmentKind
import com.github.heheteam.commonlib.LocalMediaAttachment
import com.github.heheteam.commonlib.TextWithMediaAttachments
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.studentbot.state.MenuState
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.utils.buildEntities

class L3S0(override val context: User, override val userId: StudentId) : QuestState() {
  override suspend fun BotContext.run() {
    send(
      "Вы входите в каменный лабиринт. На стенах — буквы. " +
        "Твоя задача — пройти правильно и собрать все буквы, чтобы сложить имя врага."
    )
    val buttons = listOf("\uD83D\uDE80 Я готов(а)!", "\uD83D\uDD19 Назад\n")
    send(
      "\uD83D\uDC36 Дуся: \"Говорят, если собрать их все, ты узнаешь имя моего злейшего врага. " +
        "А он ещё тот ужас... надеюсь, ты не боишься бантиков.\"\n",
      replyMarkup = horizontalKeyboard(buttons)
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

class L3S1(override val context: User, override val userId: StudentId) : QuestState() {
  override suspend fun BotContext.run() {
    send(
      TextWithMediaAttachments(
        text =
          buildEntities {
            +"Пройди лабиринт кратчайшим путем, собери буквы на этом пути и составь слово"
          },
        attachments = listOf(LocalMediaAttachment(AttachmentKind.PHOTO, "/maze.jpg")),
      )
    )
    val correctAnswer = "Вомбат".lowercase()
    addTextMessageHandler { message ->
      when (message.content.text.trim().lowercase()) {
        correctAnswer -> NewState(L3S2(context, userId))
        else -> {
          NewState(L3S1Wrong(context, userId))
        }
      }
    }
  }
}

class L3S1Wrong(override val context: User, override val userId: StudentId) : QuestState() {
  override suspend fun BotContext.run() {
    val buttons = listOf("✅ Попробовать еще раз!", "\uD83D\uDD19 Назад")
    send(
      "Неа... Это не то. Внимательно собери все буквы и попробуй снова!\n",
      replyMarkup = horizontalKeyboard(buttons),
    )
      .also { messagesWithKeyboard.add(it) }
    addDataCallbackHandler { callbackQuery ->
      when (callbackQuery.data) {
        buttons[0] -> NewState(L3S1(context, userId))
        buttons[1] -> NewState(MenuState(context, userId))
        else -> Unhandled
      }
    }
  }
}

class L3S2(override val context: User, override val userId: StudentId) : QuestState() {
  override suspend fun BotContext.run() {
    send(
      "\uD83D\uDC29 БУМ! Появляется он... \uD83D\uDCA5 ПУДЕЛЬ ИННОКЕНТИЙ! В бантиках. С калькулятором."
    )
    val buttons = listOf("\uD83D\uDE80 Вперед, в Лабиринт!", "\uD83D\uDD19 Назад\n")
    send(
      "\uD83D\uDC36 Дуся: \"Хватит болтать! Покажи ему, кто тут гений!\"\n",
      replyMarkup = horizontalKeyboard(buttons)
    ).also { messagesWithKeyboard.add(it) }
    addDataCallbackHandler { callbackQuery ->
      when (callbackQuery.data) {
        buttons[0] -> NewState(L3S3(context, userId))
        buttons[1] -> NewState(MenuState(context, userId))
        else -> Unhandled
      }
    }
  }
}

class L3S3(override val context: User, override val userId: StudentId) : QuestState() {
  override suspend fun BotContext.run() {
    send(
      "Битва: последовательность: 3 → 6 → 7 → 2.\n" +
        "Правило: чётное → влево, нечётное → вправо\n"
    )
    val buttons = listOf("17", "26", "59", "81")
    send(
      "\uD83D\uDC36 Дуся: \"Какая-то конченная задача! просто выбери кнопку с числом ${buttons[1]}\"\n",
      replyMarkup = horizontalKeyboard(buttons)
    ).also { messagesWithKeyboard.add(it) }
    addDataCallbackHandler { callbackQuery ->
      when (callbackQuery.data) {
        buttons[0] -> NewState(L3S3Wrong(context, userId))
        buttons[1] -> NewState(L3S4(context, userId))
        buttons[2] -> NewState(L3S3Wrong(context, userId))
        buttons[3] -> NewState(L3S3Wrong(context, userId))
        else -> Unhandled
      }
    }
  }
}

class L3S3Wrong(override val context: User, override val userId: StudentId) : QuestState() {
  override suspend fun BotContext.run() {
    send(
      "🐩 Иннокентий хохочет: \"Ахаха! Ну давай, попробуй ещё...\""
    )
    val buttons = listOf("\uD83D\uDD01 Попробовать снова", "\uD83D\uDD19 Назад")
    send(
      "\uD83D\uDC36 Дуся (шепчет): \"Я же тебе говорила, просто выбери нужную кнопку с числом! " +
        "\uD83D\uDE21\uD83D\uDE21\uD83D\uDE21\"\n",
      replyMarkup = horizontalKeyboard(buttons)
    ).also { messagesWithKeyboard.add(it) }
    addDataCallbackHandler { callbackQuery ->
      when (callbackQuery.data) {
        buttons[0] -> NewState(L3S3(context, userId))
        buttons[1] -> NewState(MenuState(context, userId))
        else -> Unhandled
      }
    }
  }
}

class L3S4(override val context: User, override val userId: StudentId) : QuestState() {
  override suspend fun BotContext.run() {
    send("БАБАХ! Пудель падает. Калькулятор улетает в кусты.")
    val buttons = listOf("Вперед, к финалу!", "\uD83D\uDD19 Назад")
    send(
      "\uD83D\uDC36 Дуся: \"Ты победил(а) Иннокентия! Это был наш главный враг!\n" +
        "Путь на Мозговую Гору открыт. Ты — настоящий герой Матемаланда!\"\n",
      replyMarkup = verticalKeyboard(buttons)
    ).also { messagesWithKeyboard.add(it) }
    addDataCallbackHandler { callbackQuery ->
      when (callbackQuery.data) {
        buttons[0] -> NewState(L4S0(context, userId))
        buttons[1] -> NewState(MenuState(context, userId))
        else -> Unhandled
      }
    }
  }
}

