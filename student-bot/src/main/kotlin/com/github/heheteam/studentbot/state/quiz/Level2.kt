package com.github.heheteam.studentbot.state.quiz

import com.github.heheteam.commonlib.interfaces.StudentId
import dev.inmo.tgbotapi.types.chat.User

class L2S0(override val context: User, override val userId: StudentId) : QuestState() {
  override suspend fun BotContext.run() {
    send(
      "Вы входите в каменный лабиринт. На стенах — буквы. Твоя задача — пройти правильно и собрать все буквы, чтобы сложить имя врага."
    )
    send(
      "$dogEmoji Дуся: \"Говорят, если собрать их все, ты узнаешь имя моего злейшего врага. А он ещё тот ужас... надеюсь, ты не боишься бантиков.\""
    )
    addStringReadHandler(
      "Иннокентий",
      L2Boss(context, userId),
      DefaultErrorState(context, userId, L2S0(context, userId)),
    )
  }
}

class L2Boss(override val context: User, override val userId: StudentId) : QuestState() {
  override suspend fun BotContext.run() {
    send(
      "\uD83D\uDC29 БУМ! Появляется он... \uD83D\uDCA5 ПУДЕЛЬ ИННОКЕНТИЙ! В бантиках. С калькулятором."
    )
    send(
      "\"Ррррафф! Склонитесь пред истинным гением, умнейшем среди псов! Ну что, Дуся... ты снова притащила какого-то двоечника? Ррррафф!”"
    )
    send("\uD83D\uDC36 Дуся: \"Хватит болтать! {Имя}, покажи ему, кто тут гений!\"")
    send(
      "Дуся и Иннокентий начинают битву складывания чисел! Дуся сложила два числа и получила сумму 32. После этого Иннокентий прибавил к полученной сумме ещё два числа и получил 53. Затем Дуся прибавила ещё три числа и получил в результате 88. Какое наименьшее количество чётных слагаемых могло быть среди всех 7 чисел, которые складывались на битве?"
    )

    addIntegerReadHandler(
      1,
      this@L2Boss,
      L3S0(context, userId),
      DefaultErrorState(context, userId, L3S0(context, userId)),
    )
  }
}
