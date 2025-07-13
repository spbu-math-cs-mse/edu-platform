package com.github.heheteam.studentbot.state.quiz

import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.studentbot.state.MenuState
import dev.inmo.tgbotapi.types.chat.User

class L4Final(override val context: User, override val userId: StudentId) : QuestState() {
  override suspend fun BotContext.run() {
    send("Ты стоишь на вершине. Перед тобой — сияющая звезда.\n")
    send(
      "\uD83D\uDC36 Дуся: \"Это — Звезда Дабромат. Она ждала тебя. " +
        "В ней — сила, которая помогает учиться, понимать и придумывать.\""
    )
    val buttons = listOf("\uD83C\uDFC5 Получить Сертификат", "\uD83C\uDF93 Узнать о курсе")
    send(
      "\uD83D\uDC3E \"Ты герой. А хочешь дальше решать класснее сложные задачи — " +
        "приходи к нам в онлайн-школу Дабромат! " +
        "В учебе я тебя не брошу, не переживай — буду продолжать помогать тебе. " +
        "Но помимо этого ты познакомишься с лучшими преподавателями страны, " +
        "которые прокачают твой мозг так, что ты сможешь стать интеллектуальной элитой, " +
        "зарабатывать горы денег (и подкармливать меня!) и найти много новых друзей, " +
        "столь же увлеченных решением сложных задач, как и ты! \"",
      replyMarkup = verticalKeyboard(buttons),
    )

    addDataCallbackHandler { callbackQuery ->
      when (callbackQuery.data) {
        buttons[0] -> NewState(TODO())
        buttons[1] -> NewState(TODO())
        else -> Unhandled
      }
    }
  }
}

class L4S1(override val context: User, override val userId: StudentId) : QuestState() {
  override suspend fun BotContext.run() {
    send("Терраса 1: 5 − 1 + 4 = ❓")
    val correctAnswer = 8
    addTextMessageHandler { message ->
      when (message.content.text.trim().toIntOrNull()) {
        null -> {
          send("Надо ввести число")
          NewState(L4S1(context, userId))
        }

        correctAnswer -> NewState(L4S2(context, userId))
        else -> {
          NewState(L4S1Wrong(context, userId))
        }
      }
    }
  }
}

class L4S1Wrong(override val context: User, override val userId: StudentId) : QuestState() {
  override suspend fun BotContext.run() {
    send("* Камень падает у твоих ног… *")
    val buttons = listOf("✅ Попробовать еще раз!", "\uD83D\uDD19 Назад")
    send(
        "\uD83D\uDC36 Дуся: \"Попробуй посчитать ещё раз. Мы почти на террасе!\"\n",
        replyMarkup = verticalKeyboard(buttons),
      )
      .also { messagesWithKeyboard.add(it) }
    addDataCallbackHandler { callbackQuery ->
      when (callbackQuery.data) {
        buttons[0] -> NewState(L4S1(context, userId))
        buttons[1] -> NewState(MenuState(context, userId))
        else -> Unhandled
      }
    }
  }
}

class L4S2(override val context: User, override val userId: StudentId) : QuestState() {
  override suspend fun BotContext.run() {
    send("Терраса 1 пройдена!")
    val buttons = listOf("\uD83D\uDE80 Дальше!", "\uD83D\uDD19 Назад\n")
    send(
        "\uD83D\uDC36 Дуся: \"Один этаж за спиной! Вперёд — выше, к следующей!\"\n",
        replyMarkup = horizontalKeyboard(buttons),
      )
      .also { messagesWithKeyboard.add(it) }
    addDataCallbackHandler { callbackQuery ->
      when (callbackQuery.data) {
        buttons[0] -> NewState(L4S3(context, userId))
        buttons[1] -> NewState(MenuState(context, userId))
        else -> Unhandled
      }
    }
  }
}

class L4S3(override val context: User, override val userId: StudentId) : QuestState() {
  override suspend fun BotContext.run() {
    send("Терраса 2: 3 + 2 × 4 − 1 = ❓")
    val correctAnswer = 10
    addTextMessageHandler { message ->
      when (message.content.text.trim().toIntOrNull()) {
        null -> {
          send("Надо ввести число")
          NewState(L4S3(context, userId))
        }

        correctAnswer -> NewState(L4S4(context, userId))
        else -> {
          NewState(L4S3Wrong(context, userId))
        }
      }
    }
  }
}

class L4S3Wrong(override val context: User, override val userId: StudentId) : QuestState() {
  override suspend fun BotContext.run() {
    val buttons = listOf("\uD83D\uDD01 Попробовать еще раз", "\uD83D\uDD19 Назад")
    send("Ветер подул сильнее. Дуся хватается за уши.", replyMarkup = verticalKeyboard(buttons))
      .also { messagesWithKeyboard.add(it) }
    addDataCallbackHandler { callbackQuery ->
      when (callbackQuery.data) {
        buttons[0] -> NewState(L4S3(context, userId))
        buttons[1] -> NewState(MenuState(context, userId))
        else -> Unhandled
      }
    }
  }
}

class L4S4(override val context: User, override val userId: StudentId) : QuestState() {
  override suspend fun BotContext.run() {
    val buttons = listOf("Последняя задача!", "\uD83D\uDD19 Назад")
    send("Терраса 2 пройдена!", replyMarkup = verticalKeyboard(buttons)).also {
      messagesWithKeyboard.add(it)
    }
    addDataCallbackHandler { callbackQuery ->
      when (callbackQuery.data) {
        buttons[0] -> NewState(L4S5(context, userId))
        buttons[1] -> NewState(MenuState(context, userId))
        else -> Unhandled
      }
    }
  }
}

class L4S5(override val context: User, override val userId: StudentId) : QuestState() {
  override suspend fun BotContext.run() {
    send("Терраса 3: Больше 30, меньше 40, точный квадрат = ❓")
    val correctAnswer = 36
    addTextMessageHandler { message ->
      when (message.content.text.trim().toIntOrNull()) {
        null -> {
          send("Надо ввести число")
          NewState(L4S5(context, userId))
        }

        correctAnswer -> NewState(LFS0(context, userId))
        else -> {
          NewState(L4S5Wrong(context, userId))
        }
      }
    }
  }
}

class L4S5Wrong(override val context: User, override val userId: StudentId) : QuestState() {
  override suspend fun BotContext.run() {
    val buttons = listOf("\uD83D\uDD01 Попробовать еще раз", "\uD83D\uDD19 Назад")
    send("Ветер подул сильнее. Дуся хватается за уши.", replyMarkup = verticalKeyboard(buttons))
      .also { messagesWithKeyboard.add(it) }
    addDataCallbackHandler { callbackQuery ->
      when (callbackQuery.data) {
        buttons[0] -> NewState(L4S5(context, userId))
        buttons[1] -> NewState(MenuState(context, userId))
        else -> Unhandled
      }
    }
  }
}
