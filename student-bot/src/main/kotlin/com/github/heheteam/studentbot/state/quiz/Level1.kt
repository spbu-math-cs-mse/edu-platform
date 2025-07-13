package com.github.heheteam.studentbot.state.quiz

import com.github.heheteam.commonlib.AttachmentKind
import com.github.heheteam.commonlib.LocalMediaAttachment
import com.github.heheteam.commonlib.TextWithMediaAttachments
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
        "\uD83D\uDC36 Дуся: \"Вместе мы пройдём сквозь чащу и найдём тайный выход! " +
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
    send("${dogEmoji} Дуся: \"Осторожно! Только тот, кто решит задачу, может пройти дальше.\"")
    send(
      "На очень длинных воротах кто-то выписал все числа от 1 до 25 в порядке убывания без пробелов, " +
        "так что получилось очень большое число: 252423222120...54321. " +
        "Какая цифра записана на 24 месте, считая слева направо?"
    )

    addTextMessageHandler { message ->
      when (message.content.text.trim().toIntOrNull()) {
        null -> {
          send("Надо ввести число")
          NewState(L1S1(context, userId))
        }

        4 -> NewState(L1S2(context, userId))
        else -> {
          NewState(L1S1Wrong(context, userId))
        }
      }
    }
  }
}

class L1S1Wrong(override val context: User, override val userId: StudentId) : QuestState() {
  override suspend fun BotContext.run() {
    val buttons = listOf("✅ Конечно!", "\uD83D\uDD19 Назад")
    send(
        "\uD83D\uDD12 Ворота задрожали… но остались закрыты. " +
          "Наверное, ответ был неверный. Деревья недовольно зашумели.\n Попробуем ещё раз? ",
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
        "\uD83D\uDC36 Дуся: \"Кстати... хочешь почесать мне пузо? Я это очень люблю!\"\n",
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
    val content =
      TextWithMediaAttachments(
        attachments = listOf(LocalMediaAttachment(AttachmentKind.DOCUMENT, "/maze_correct.mp4"))
      )
    val buttons = listOf("\uD83C\uDFDE Перейти к речке")
    send(content)
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
    val buttons = listOf("\uD83C\uDFDE Перескочить речку", "\uD83D\uDD19  Назад")
    send(
        "\uD83D\uDC36 Дуся: \"Ты справишься! Главное — не оступиться!\"",
        replyMarkup = verticalKeyboard(buttons),
      )
      .also { messagesWithKeyboard.add(it) }
    val trueAnswer = 7
    addIntegerReadHandler(
      trueAnswer,
      this@L1S3,
      L1S4(context, userId),
      DefaultErrorState(context, userId, this@L1S3),
    )
  }
}

// class L1S3Wrong(override val context: User, override val userId: StudentId) : QuestState() {
//  override suspend fun BotContext.run() {
//    val buttons = listOf("✅ Конечно!", "\uD83D\uDD19 Назад")
//    send(
//        "*Бульк!* — чуть не оступился! " +
//          "Цепочка чисел была неправильная — надо попробовать ещё раз, чтобы не промокнуть!\n" +
//          "Попробуем ещё раз?",
//        replyMarkup = verticalKeyboard(buttons),
//      )
//      .also { messagesWithKeyboard.add(it) }
//    addDataCallbackHandler { callbackQuery ->
//      when (callbackQuery.data) {
//        buttons[0] -> NewState(L1S31(context, userId))
//        buttons[1] -> NewState(MenuState(context, userId))
//        else -> Unhandled
//      }
//    }
//  }
// }

class L1S4(override val context: User, override val userId: StudentId) : QuestState() {
  override suspend fun BotContext.run() {
    send("Ты перепрыгнул речку! \uD83C\uDF89 Вас встречает древний говорящий дуб.\n")
    val treeEmoji = "🌳"
    send("$treeEmoji Я не дерево. Я ЭНТ! Никто не пройдет дальше. Это моя дорога, и она платная")
    send(
      "$dogEmoji Дуся (шёпотом): \"(Поскуливает) Но у нас нет денег — я собака, а это человеческий щенок…\""
    )
    send(
      "$treeEmoji Чтож. Вы можете помочь мне и иначе — у меня очень сильно чешется голова: " +
        "в листьях я своих запутался, которых аж 500. " +
        "Часть листьев у меня жадные: они любят отнимать — их у меня 300. " +
        "Часть листьев у меня предприимчивые: они любят делить, таких у меня 400. " +
        "Понять мне надо бы, сколько листьев одновременно любят и отнимать, и делить —  " +
        "они красного цвета уже, надо их сбрасывать."
    )

    val buttons =
      listOf("\uD83E\uDDE0 Подойти к дубу", "\uD83E\uDD17 Почесать ещё раз", "\uD83D\uDD19  Назад")
    send(
        "\uD83D\uDC36 Дуся: \"А пока ты отдыхаешь, может, почешешь мне пузо? Ну пожааалуйста! \uD83E\uDD7A\"\n",
        replyMarkup = verticalKeyboard(buttons),
      )
      .also { messagesWithKeyboard.add(it) }
    addDataCallbackHandler { callbackQuery ->
      when (callbackQuery.data) {
        buttons[0] -> NewState(L2S0(context, userId))
        buttons[1] -> NewState(L1S4Bellyrub(context, userId))
        buttons[2] -> NewState(MenuState(context, userId))
        else -> Unhandled
      }
    }
  }
}

class L1S4Bellyrub(override val context: User, override val userId: StudentId) : QuestState() {
  override suspend fun BotContext.run() {
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
