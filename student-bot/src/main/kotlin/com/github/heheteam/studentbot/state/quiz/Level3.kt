package com.github.heheteam.studentbot.state.quiz

import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.studentbot.state.MenuState
import dev.inmo.tgbotapi.types.chat.User
import kotlinx.coroutines.delay

class L3S0(override val context: User, override val userId: StudentId) : QuestState() {
  override suspend fun BotContext.run() {
    send(
      "\uD83D\uDC36 Дуся: \"Наверху — то, зачем мы пришли. Звезда. Осталось преодолеть три склона — и мы на месте!\"\n\nСклон 1:"
    )
    send(
      "\uD83D\uDC36 Дуся: \"Смотри, подарки! Чую, в одном из них должно быть что-то вкусное, давай его вскроем!\""
    )
    send(
      "Вы видите несколько подарков. В одном из подарков, аппетитная косточка, а в остальных грязный носок. " +
        "Рядом инструкция: косточка в коробке, которая обладает ровно двумя из трех перечисленных свойств:\n" +
        "• он красный \n" +
        "• он не круглый \n" +
        "• у него есть бантик.\n" +
        "Какой подарок нужно открыть?\n"
    )

    addStringReadHandler(
      "Г",
      L3S1(context, userId),
      DefaultErrorState(context, userId, L3S1(context, userId)),
    )
  }
}

class L3S1(override val context: User, override val userId: StudentId) : QuestState() {
  override suspend fun BotContext.run() {
    send("\uD83D\uDC36 Дуся: \"Хм… Ты правда думаешь, что ЭТО вкусно пахнет?.\"")
    send(
      "Склон 2: Заходим на второй склон. " +
        "По террасе прыгают и мяукают двузначные числа, но не обычные, " +
        "а все такие, что если вычеркнуть из числа одну цифру, то мы получим число, " +
        "которое делится на 3. Сколько всего чисел на террасе?"
    )
    addIntegerReadHandler(
      48,
      this@L3S1,
      L3S2(context, userId),
      DefaultErrorState(context, userId, this@L3S1),
    )
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
    send("\uD83D\uDC36 Дуся: \"Размяукались тут! Ну ничего, мы вас пересчитали и скоро вернемся…\"")
    send("Склон 3:")
    send(
      "На последнем склоне расположена дума, в которой за круглым столом заседают лесные депутаты."
    )
    val timeMillis = 500L
    delay(timeMillis)
    send("“Стой, кто идет, не положено!”")
    delay(timeMillis)
    send("Кнопка: “Здравствуйте, уважаемые лесные депутаты, нам очень нужно пройти дальше”")
    delay(timeMillis)
    send("Депутаты: “Не положено по закону никому проходить к звезде, дабы не случилось чего!!”")
    delay(timeMillis)
    send(
      "Такса Дуся: “Вы же лесные депутаты, вы принимаете законы. А можете придумать для нас такой закон, чтобы можно было пройти к звезде?”"
    )
    delay(timeMillis)
    send(
      "Депутаты: “Это в общем и целом, возможно, но есть одна беда. Оказывается, среди нас есть несколько нечестных депутатов! Мы очень стараемся понять, сколько их, но все у нас не выходит. А пока мы не знаем этого, новые законы мы принимать не сможем”"
    )
    delay(timeMillis)
    send(
      "Такса Дуся: “Со мной крайне одаренный в математике ученик! Он с легкостью разгадает, кто из вас тут честный, рраафф!”"
    )
    delay(timeMillis)
    send(
      "За круглым столом собрались 30 лесных депутатов, которые охраняют звезду \"Дабы не случилось чего\". " +
        "Всякий депутат либо честный, либо нечестный. " +
        "Каждый честный депутат может говорить только правду, а каждый нечестный – только ложь. " +
        "Каждый из них сказал \"Слева от меня сидят 2 нечестных депутата\". " +
        "Какое максимальное количество честных депутатов могло сидеть за круглым столом?"
    )

    addIntegerReadHandler(
      9,
      this@L3S2,
      L4Final(context, userId),
      DefaultErrorState(context, userId, this@L3S2),
    )
  }
}

class L3S3(override val context: User, override val userId: StudentId) : QuestState() {
  override suspend fun BotContext.run() {
    send(
      "Битва: последовательность: 3 → 6 → 7 → 2.\n" + "Правило: чётное → влево, нечётное → вправо\n"
    )
    val buttons = listOf("17", "26", "59", "81")
    send(
        "\uD83D\uDC36 Дуся: \"Какая-то конченная задача! просто выбери кнопку с числом ${buttons[1]}\"\n",
        replyMarkup = horizontalKeyboard(buttons),
      )
      .also { messagesWithKeyboard.add(it) }
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
    send("🐩 Иннокентий хохочет: \"Ахаха! Ну давай, попробуй ещё...\"")
    val buttons = listOf("\uD83D\uDD01 Попробовать снова", "\uD83D\uDD19 Назад")
    send(
        "\uD83D\uDC36 Дуся (шепчет): \"Я же тебе говорила, просто выбери нужную кнопку с числом! " +
          "\uD83D\uDE21\uD83D\uDE21\uD83D\uDE21\"\n",
        replyMarkup = horizontalKeyboard(buttons),
      )
      .also { messagesWithKeyboard.add(it) }
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
        replyMarkup = verticalKeyboard(buttons),
      )
      .also { messagesWithKeyboard.add(it) }
    addDataCallbackHandler { callbackQuery ->
      when (callbackQuery.data) {
        buttons[0] -> NewState(L4Final(context, userId))
        buttons[1] -> NewState(MenuState(context, userId))
        else -> Unhandled
      }
    }
  }
}
