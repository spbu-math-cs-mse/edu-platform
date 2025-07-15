package com.github.heheteam.studentbot.state.quiz

import com.github.heheteam.commonlib.api.CommonUserApi
import com.github.heheteam.commonlib.api.ParentApi
import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.interfaces.CommonUserId
import com.github.heheteam.commonlib.interfaces.ParentId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import dev.inmo.tgbotapi.types.chat.User
import kotlinx.coroutines.delay

open class L3S0<ApiService : CommonUserApi<UserId>, UserId : CommonUserId>(
  override val context: User,
  override val userId: UserId,
) : QuestState<ApiService, UserId>() {
  override suspend fun BotContext.run(service: ApiService) {
    saveState(service)
    send(
      "$DOG_EMOJI Дуся: \"Наверху — то, зачем мы пришли. Звезда. " +
        "Осталось преодолеть три склона — и мы на месте!\""
    )
    sendMarkdown("*Склон 1*")
    send(
      "$DOG_EMOJI Дуся: \"Смотри, подарки! Чую, в одном из них должно быть что-то вкусное, давай его вскроем!\""
    )
    sendImage("/gifts.png")
    val buttons = listOf("А", "Б", "В", "Г")
    send(
        "Вы видите несколько подарков. В одном из подарков, аппетитная косточка, а в остальных грязный носок. " +
          "Рядом инструкция: косточка в коробке, которая обладает ровно двумя из трех перечисленных свойств:\n" +
          "• он красный \n" +
          "• он не круглый \n" +
          "• у него есть бантик.\n" +
          "Какой подарок нужно открыть?\n",
        replyMarkup = verticalKeyboard(buttons),
      )
      .also { messagesWithKeyboard.add(it) }
    addDataCallbackHandler { callbackQuery ->
      when (callbackQuery.data) {
        buttons[3] -> {
          send("$DOG_EMOJI Дуся: \"Ням-ням! Спасибо тебе за косточку!\"")
          val userId = userId
          NewState(
            when (userId) {
              is StudentId -> L3S1Student(context, userId)
              is ParentId -> L3S1Parent(context, userId)
              else -> error("unreachable")
            }
          )
        }

        in buttons -> {
          send("$DOG_EMOJI Дуся: \"Хм… Ты правда думаешь, что ЭТО вкусно пахнет?\"")
          val userId = userId
          NewState(
            when (userId) {
              is StudentId ->
                DefaultErrorStateStudent(context, userId, L3S0Student(context, userId))
              is ParentId -> DefaultErrorStateParent(context, userId, L3S0Parent(context, userId))
              else -> error("unreachable")
            }
          )
        }

        else -> Unhandled
      }
    }
  }
}

private const val L3S1_ANSWER = 54

open class L3S1<ApiService : CommonUserApi<UserId>, UserId : CommonUserId>(
  override val context: User,
  override val userId: UserId,
) : QuestState<ApiService, UserId>() {
  override suspend fun BotContext.run(service: ApiService) {
    saveState(service)
    sendMarkdown("*Склон 2*")
    sendImage("/cats.png")
    send(
      "По террасе прыгают и мяукают двузначные числа, но не обычные, " +
        "а все такие, что если вычеркнуть из числа одну цифру, то мы получим число, " +
        "которое делится на 3. Сколько всего чисел на террасе?"
    )
    addIntegerReadHandler(
      L3S1_ANSWER,
      this@L3S1,
      {
        send(
          "$DOG_EMOJI Дуся: \"Размяукались тут! Ну ничего, мы вас пересчитали и скоро вернемся…\""
        )
        val userId = userId
        when (userId) {
          is StudentId -> L3S2Student(context, userId)
          is ParentId -> L3S2Parent(context, userId)
          else -> error("unreachable")
        }
      },
      {
        send("$DOG_EMOJI Дуся: \"Тяф! Даже ежу понятно, что ты не прав!\"\n")
        val userId = userId
        when (userId) {
          is StudentId -> DefaultErrorStateStudent(context, userId, L3S1Student(context, userId))
          is ParentId -> DefaultErrorStateParent(context, userId, L3S1Parent(context, userId))
          else -> error("unreachable")
        }
      },
    )
  }
}

private const val L3S2_ANSWER = 10

open class L3S2<ApiService : CommonUserApi<UserId>, UserId : CommonUserId>(
  override val context: User,
  override val userId: UserId,
) : QuestState<ApiService, UserId>() {
  override suspend fun BotContext.run(service: ApiService) {
    saveState(service)
    sendMarkdown("*Склон 3*")
    sendImage("/deputies.png")
    send(
      "На последнем склоне расположена дума, в которой за круглым столом заседают лесные депутаты."
    )
    val timeMillis = 500L
    delay(timeMillis)
    send("\"Стой, кто идет, не положено!\"")
    delay(timeMillis)
    send(
      "$DOG_EMOJI Дуся: \"Здравствуйте, уважаемые лесные депутаты, нам очень нужно пройти дальше\""
    )
    delay(timeMillis)
    send("Депутаты: \"Не положено по закону никому проходить к звезде, дабы не случилось чего!!\"")
    delay(timeMillis)
    send(
      "$DOG_EMOJI Дуся: \"Вы же лесные депутаты, вы принимаете законы. " +
        "А можете придумать для нас такой закон, чтобы можно было пройти к звезде?\""
    )
    delay(timeMillis)
    send(
      "Депутаты: \"Это в общем и целом, возможно, но есть одна беда. " +
        "Оказывается, среди нас есть несколько нечестных депутатов! " +
        "Мы очень стараемся понять, сколько их, но все у нас не выходит. " +
        "А пока мы не знаем этого, новые законы мы принимать не сможем\""
    )
    delay(timeMillis)
    send(
      "$DOG_EMOJI Дуся: \"Со мной крайне одаренный в математике ученик! " +
        "Он с легкостью разгадает, кто из вас тут честный, рраафф!\""
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
      L3S2_ANSWER,
      this@L3S2,
      {
        send("$DOG_EMOJI Дуся: \"Победа! Это почти вершина... вижу свет звезды!\"")
        val userId = userId
        when (userId) {
          is StudentId -> L4FinalStudent(context, userId)
          is ParentId -> L4FinalParent(context, userId)
          else -> error("unreachable")
        }
      },
      {
        send(
          "$DOG_EMOJI Дуся: \"Ээээ… таксы в политике ничего не понимают, но тут ясен пень ты ошибся!\""
        )
        val userId = userId
        when (userId) {
          is StudentId -> DefaultErrorStateStudent(context, userId, L3S2Student(context, userId))
          is ParentId -> DefaultErrorStateParent(context, userId, L3S2Parent(context, userId))
          else -> error("unreachable")
        }
      },
    )
  }
}

class L3S0Student(context: User, userId: StudentId) : L3S0<StudentApi, StudentId>(context, userId)

class L3S0Parent(context: User, userId: ParentId) : L3S0<ParentApi, ParentId>(context, userId)

class L3S1Student(context: User, userId: StudentId) : L3S1<StudentApi, StudentId>(context, userId)

class L3S1Parent(context: User, userId: ParentId) : L3S1<ParentApi, ParentId>(context, userId)

class L3S2Student(context: User, userId: StudentId) : L3S2<StudentApi, StudentId>(context, userId)

class L3S2Parent(context: User, userId: ParentId) : L3S2<ParentApi, ParentId>(context, userId)
