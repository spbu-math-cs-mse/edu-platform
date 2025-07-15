package com.github.heheteam.studentbot.state.quiz

import com.github.heheteam.commonlib.api.CommonUserApi
import com.github.heheteam.commonlib.api.ParentApi
import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.interfaces.CommonUserId
import com.github.heheteam.commonlib.interfaces.ParentId
import com.github.heheteam.commonlib.interfaces.StudentId
import dev.inmo.tgbotapi.types.chat.User

open class L2S0<ApiService : CommonUserApi<UserId>, UserId : CommonUserId>(
  override val context: User,
  override val userId: UserId,
) : QuestState<ApiService, UserId>() {
  override suspend fun BotContext.run(service: ApiService) {
    sendImage("/maze.png")
    send(
      "Вы входите в каменный лабиринт. На стенах — буквы. " +
        "Твоя задача — пройти правильно и собрать все буквы, чтобы сложить имя врага."
    )
    send(
      "$DOG_EMOJI Дуся: \"Говорят, если собрать их все, ты узнаешь имя моего злейшего врага. " +
        "А он ещё тот ужас... надеюсь, ты не боишься бантиков.\""
    )
    addStringReadHandler(
      "Иннокентий",
      {
        val userId = userId
        when (userId) {
          is StudentId -> L2BossStudent(context, userId)
          is ParentId -> L2BossParent(context, userId)
          else -> error("unreachable")
        }
      },
      {
        send(
          "$DOG_EMOJI Дуся: \"Ах-ах, что за имя такое вышло, кошачье что ли? " +
            "Наверное мы в какой-то момент пошли не туда…\""
        )
        val userId = userId
        when (userId) {
          is StudentId -> DefaultErrorStateStudent(context, userId, this@L2S0)
          is ParentId -> DefaultErrorStateParent(context, userId, this@L2S0)
          else -> error("unreachable")
        }
      },
    )
  }
}

open class L2Boss<ApiService : CommonUserApi<UserId>, UserId : CommonUserId>(
  override val context: User,
  override val userId: UserId,
) : QuestState<ApiService, UserId>() {
  override suspend fun BotContext.run(service: ApiService) {
    saveState(service)
    sendImage("/innokenty.png")
    send(
      "\uD83D\uDC29 БУМ! Появляется он... \uD83D\uDCA5 ПУДЕЛЬ ИННОКЕНТИЙ! В бантиках. С калькулятором."
    )
    send(
      "\"Ррррафф! Склонитесь пред истинным гением, умнейшем среди псов! " +
        "Ну что, Дуся... ты снова притащила какого-то двоечника? Ррррафф!”"
    )
    send("$DOG_EMOJI Дуся: \"Хватит болтать! Покажи ему, кто тут гений!\"")
    send(
      "Дуся и Иннокентий начинают битву складывания чисел! " +
        "Дуся сложила два числа и получила сумму 32. " +
        "После этого Иннокентий прибавил к полученной сумме ещё два числа и получил 53. " +
        "Затем Дуся прибавила ещё три числа и получил в результате 88. " +
        "Какое наименьшее количество чётных слагаемых могло быть среди всех 7 чисел, которые складывались на битве?"
    )

    addIntegerReadHandler(
      1,
      this@L2Boss,
      {
        sendImage("/loser_innokenty.png")
        send("$DOG_EMOJI Дуся: \"Аф-аф! Враг повержен, знай наших!\"")
        val userId = userId
        when (userId) {
          is StudentId -> L3S0Student(context, userId)
          is ParentId -> L3S0Parent(context, userId)
          else -> error("unreachable")
        }
      },
      {
        send("\uD83D\uDC29 Иннокентий хохочет: \"Ахаха! Ну давай, попробуй ещё...\"")
        sendImage("/loser_dusya.png")
        send(
          "$DOG_EMOJI Дуся (шепчет): \"Ай-ай, больно! Попробуй ещё, только следи внимательнее!\""
        )
        val userId = userId
        when (userId) {
          is StudentId -> DefaultErrorStateStudent(context, userId, this@L2Boss)
          is ParentId -> DefaultErrorStateParent(context, userId, this@L2Boss)
          else -> error("unreachable")
        }
      },
    )
  }
}

class L2S0Student(context: User, userId: StudentId) : L2S0<StudentApi, StudentId>(context, userId)

class L2S0Parent(context: User, userId: ParentId) : L2S0<ParentApi, ParentId>(context, userId)

class L2BossStudent(context: User, userId: StudentId) :
  L2Boss<StudentApi, StudentId>(context, userId)

class L2BossParent(context: User, userId: ParentId) : L2Boss<ParentApi, ParentId>(context, userId)
