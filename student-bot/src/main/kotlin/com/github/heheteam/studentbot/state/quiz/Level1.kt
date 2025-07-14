package com.github.heheteam.studentbot.state.quiz

import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.studentbot.state.MenuState
import dev.inmo.tgbotapi.types.chat.User

class L1S0(override val context: User, override val userId: StudentId) : QuestState() {
  override suspend fun BotContext.run() {
    send(
      "\uD83C\uDF32 Ты входишь в Числовой Лес. " +
        "Всё здесь построено из чисел: деревья считают листья, кусты шепчут примеры."
    )

    val buttons = listOf("\uD83D\uDE80 Да, откроем ворота!", "\uD83D\uDD19 Назад")
    send(
        "$DOG_EMOJI Дуся: \"Вместе мы пройдём сквозь чащу и найдём тайный выход! " +
          "Но сначала — откроем ворота. Только тот, кто решит задачу, может пройти дальше.\"\n",
        replyMarkup = verticalKeyboard(buttons),
      )
      .also { messagesWithKeyboard.add(it) }

    addDataCallbackHandler { callbackQuery ->
      when (callbackQuery.data) {
        buttons[0] -> NewState(L1S1(context, userId))
        buttons[1] -> NewState(MenuState(context, userId))
        else -> Unhandled
      }
    }
  }
}

class L1S1(override val context: User, override val userId: StudentId) : QuestState() {
  override suspend fun BotContext.run() {
    send("$DOG_EMOJI Дуся: \"Осторожно! Только тот, кто решит задачу, может пройти дальше.\"")
    send(
      "На очень длинных воротах кто-то выписал все числа от 1 до 25 в порядке убывания без пробелов, " +
        "так что получилось очень большое число: 252423222120...54321. " +
        "Какая цифра записана на 24 месте, считая слева направо?"
    )

    addIntegerReadHandler(
      4,
      this@L1S1,
      {
        send(
          "✅ Щёлк! — ворота распахнулись, и ты входишь в чащу. " +
            "Тропинка уходит вперёд, но вдруг ты слышишь плеск воды..."
        )
        L1S2(context, userId)
      },
      {
        send(
          "\uD83D\uDD12 Ворота задрожали… но остались закрыты. " +
            "Наверное, ответ был неверный. Деревья недовольно зашумели."
        )
        DefaultErrorState(context, userId, this@L1S1)
      },
    )
  }
}

class L1S2(override val context: User, override val userId: StudentId) : QuestState() {
  override suspend fun BotContext.run() {
    send(
      "\uD83C\uDF0A Перед тобой — речка. " +
        "Через неё можно перебраться только по камням, если прыгать по ним в нужном порядке."
    )
    val buttons =
      listOf(
        "\uD83C\uDFDE Перейти к речке",
        "\uD83E\uDD17 Почесать пузо Дусе",
        "\uD83D\uDD19  Назад",
      )
    send(
        "$DOG_EMOJI Дуся: \"Кстати... хочешь почесать мне пузо? Я это очень люблю!\"\n",
        replyMarkup = verticalKeyboard(buttons),
      )
      .also { messagesWithKeyboard.add(it) }
    addDataCallbackHandler { callbackQuery ->
      when (callbackQuery.data) {
        buttons[0] -> NewState(L1S3(context, userId))
        buttons[1] -> NewState(L1S3Bellyrub(context, userId))
        buttons[2] -> NewState(MenuState(context, userId))
        else -> Unhandled
      }
    }
  }
}

class L1S3Bellyrub(override val context: User, override val userId: StudentId) : QuestState() {
  override suspend fun BotContext.run() {
    sendImage("/bellyrub_1.png")
    val buttons = listOf("\uD83C\uDFDE Перейти к речке")
    send(
        "Мррр... \uD83D\uDE0C Спасибо! Это было чудесно. " +
          "Теперь у меня ещё больше сил пройти речку вместе с тобой!",
        replyMarkup = verticalKeyboard(buttons),
      )
      .also { messagesWithKeyboard.add(it) }
    addDataCallbackHandler { callbackQuery ->
      when (callbackQuery.data) {
        buttons[0] -> NewState(L1S3(context, userId))
        else -> Unhandled
      }
    }
  }
}

class L1S3(override val context: User, override val userId: StudentId) : QuestState() {
  override suspend fun BotContext.run() {
    send(
      "\uD83E\uDEA8 Ты стоишь на берегу и видишь перед собой 101 камешек выложенных в ряд. " +
        "Можно делать либо короткие прыжки через 4 камешка, либо длинные – через 12 " +
        "(то есть если ты стоял на первом камешке, то ты можешь прыгнуть на шестой или четырнадцатый камешек). " +
        "Изначально ты стоишь на первом камешке, а последним прыжком нужно оказаться на 101 камешке. " +
        "Какое минимальное количество коротких прыжков нужно сделать? "
    )
    send("$DOG_EMOJI Дуся: \"Ты справишься! Главное — не оступиться!\"")
    addIntegerReadHandler(
      7,
      this@L1S3,
      { L1S4(context, userId) },
      {
        sendMarkdown("*Бульк!* — чуть не оступился!")
        DefaultErrorState(context, userId, this@L1S3)
      },
    )
  }
}

class L1S4(override val context: User, override val userId: StudentId) : QuestState() {
  override suspend fun BotContext.run() {
    send("Ты перепрыгнул речку! \uD83C\uDF89 Вас встречает древний говорящий дуб.\n")
    val buttons =
      listOf("\uD83E\uDDE0 Подойти к дубу", "\uD83E\uDD17 Почесать ещё раз", "\uD83D\uDD19  Назад")
    send(
        "$DOG_EMOJI Дуся: \"А пока ты отдыхаешь, может, почешешь мне пузо? Ну пожааалуйста! \uD83E\uDD7A\"\n",
        replyMarkup = verticalKeyboard(buttons),
      )
      .also { messagesWithKeyboard.add(it) }
    addDataCallbackHandler { callbackQuery ->
      when (callbackQuery.data) {
        buttons[0] -> NewState(L1S5(context, userId))
        buttons[1] -> NewState(L1S4Bellyrub(context, userId))
        buttons[2] -> NewState(MenuState(context, userId))
        else -> Unhandled
      }
    }
  }
}

class L1S4Bellyrub(override val context: User, override val userId: StudentId) : QuestState() {
  override suspend fun BotContext.run() {
    sendImage("/bellyrub_2.png")
    val buttons = listOf("\uD83C\uDFDE Подойти к дубу")
    send(
        "Ах, да! Ты — самый лучший пузочесатель на свете! \uD83D\uDC3E",
        replyMarkup = verticalKeyboard(buttons),
      )
      .also { messagesWithKeyboard.add(it) }
    addDataCallbackHandler { callbackQuery ->
      when (callbackQuery.data) {
        buttons[0] -> NewState(L2S0(context, userId))
        else -> Unhandled
      }
    }
  }
}

class L1S5(override val context: User, override val userId: StudentId) : QuestState() {
  override suspend fun BotContext.run() {
    sendImage("/ent.png")
    val treeEmoji = "🌳"
    send(
      "$treeEmoji: \"Я не дерево. Я ЭНТ! Никто не пройдет дальше. Это моя дорога, и она платная\""
    )
    send(
      "$DOG_EMOJI Дуся (шёпотом): \"(Поскуливает) Но у нас нет денег — я собака, а это человеческий щенок…\""
    )
    send(
      "$treeEmoji: \"Чтож. Вы можете помочь мне и иначе — у меня очень сильно чешется голова: " +
        "в листьях я своих запутался, которых аж 500. " +
        "Часть листьев у меня жадные: они любят отнимать — их у меня 300. " +
        "Часть листьев у меня предприимчивые: они любят делить, таких у меня 400. " +
        "Понять мне надо бы, сколько листьев одновременно любят и отнимать, и делить —  " +
        "они красного цвета уже, надо их сбрасывать.\""
    )

    val buttons = listOf("\uD83C\uDFDE Перескочить речку", "\uD83D\uDD19  Назад")
    send(
        "$DOG_EMOJI Дуся: \"Ты справишься! Главное — не оступиться!\"",
        replyMarkup = verticalKeyboard(buttons),
      )
      .also { messagesWithKeyboard.add(it) }
    addIntegerReadHandler(
      7,
      this@L1S5,
      {
        sendImage("/leaving_forest.png")
        send(
          "$treeEmoji: \"Листья молвят, что ты не ошибся. " +
            "Спасибо тебе за помощь, человеческое дитя! Теперь ты можешь идти дальше…\""
        )
        L2S0(context, userId)
      },
      { DefaultErrorState(context, userId, this@L1S5) },
    )
  }
}
