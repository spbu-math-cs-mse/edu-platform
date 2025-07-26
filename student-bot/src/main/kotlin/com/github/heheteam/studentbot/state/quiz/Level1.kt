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

open class L1S0<ApiService : CommonUserApi<UserId>, UserId : CommonUserId>(
  override val context: User,
  override val userId: UserId,
) : QuestState<ApiService, UserId>() {
  override suspend fun BotContext.run(service: ApiService) {
    saveState(service)
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
        buttons[0] -> {
          val userId = userId
          NewState(
            when (userId) {
              is StudentId -> L1S1Student(context, userId)
              is ParentId -> L1S1Parent(context, userId)
              else -> error("unreachable")
            }
          )
        }
        buttons[1] -> {
          saveState(service)
          NewState(menuState())
        }
        else -> Unhandled
      }
    }
  }
}

open class L1S1<ApiService : CommonUserApi<UserId>, UserId : CommonUserId>(
  override val context: User,
  override val userId: UserId,
) : QuestState<ApiService, UserId>() {
  override suspend fun BotContext.run(service: ApiService) {
    saveState(service)
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
        val userId = userId
        when (userId) {
          is StudentId -> L1S2Student(context, userId)
          is ParentId -> L1S2Parent(context, userId)
          else -> error("unreachable")
        }
      },
      {
        send(
          "\uD83D\uDD12 Ворота задрожали… но остались закрыты. " +
            "Наверное, ответ был неверный. Деревья недовольно зашумели."
        )
        val userId = userId
        when (userId) {
          is StudentId -> DefaultErrorStateStudent(context, userId, L1S1Student(context, userId))
          is ParentId -> DefaultErrorStateParent(context, userId, L1S1Parent(context, userId))
          else -> error("unreachable")
        }
      },
    )
  }
}

open class L1S2<ApiService : CommonUserApi<UserId>, UserId : CommonUserId>(
  override val context: User,
  override val userId: UserId,
) : QuestState<ApiService, UserId>() {
  override suspend fun BotContext.run(service: ApiService) {
    saveState(service)
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
        buttons[0] -> {
          val userId = userId
          NewState(
            when (userId) {
              is StudentId -> L1S3Student(context, userId)
              is ParentId -> L1S3Parent(context, userId)
              else -> error("unreachable")
            }
          )
        }
        buttons[1] -> {
          val userId = userId
          NewState(
            when (userId) {
              is StudentId -> L1S3BellyrubStudent(context, userId)
              is ParentId -> L1S3BellyrubParent(context, userId)
              else -> error("unreachable")
            }
          )
        }
        buttons[2] -> {
          NewState(menuState())
        }
        else -> Unhandled
      }
    }
  }
}

open class L1S3Bellyrub<ApiService : CommonUserApi<UserId>, UserId : CommonUserId>(
  override val context: User,
  override val userId: UserId,
) : QuestState<ApiService, UserId>() {
  override suspend fun BotContext.run(service: ApiService) {
    saveState(service)
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
        buttons[0] -> {
          val userId = userId
          NewState(
            when (userId) {
              is StudentId -> L1S3Student(context, userId)
              is ParentId -> L1S3Parent(context, userId)
              else -> error("unreachable")
            }
          )
        }
        else -> Unhandled
      }
    }
  }
}

open class L1S3<ApiService : CommonUserApi<UserId>, UserId : CommonUserId>(
  override val context: User,
  override val userId: UserId,
) : QuestState<ApiService, UserId>() {
  override suspend fun BotContext.run(service: ApiService) {
    saveState(service)
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
      {
        send("Ты перепрыгнул речку! \uD83C\uDF89 Вас встречает древний говорящий дуб.\n")
        val userId = userId
        when (userId) {
          is StudentId -> L1S4Student(context, userId)
          is ParentId -> L1S4Parent(context, userId)
          else -> error("unreachable")
        }
      },
      {
        sendMarkdown("*Бульк!* — чуть не оступился!")
        val userId = userId
        when (userId) {
          is StudentId -> DefaultErrorStateStudent(context, userId, L1S3Student(context, userId))
          is ParentId -> DefaultErrorStateParent(context, userId, L1S3Parent(context, userId))
          else -> error("unreachable")
        }
      },
    )
  }
}

const val TREE_EMOJI = "🌳"

open class L1S4<ApiService : CommonUserApi<UserId>, UserId : CommonUserId>(
  override val context: User,
  override val userId: UserId,
) : QuestState<ApiService, UserId>() {
  override suspend fun BotContext.run(service: ApiService) {
    saveState(service)
    sendImage("/ent.png")
    send(
      "$TREE_EMOJI Энт: \"Я не дерево. Я ЭНТ! Никто не пройдет дальше. Это моя дорога, и она платная\""
    )
    send(
      "$DOG_EMOJI Дуся (шёпотом): \"(Поскуливает) Но у нас нет денег — я собака, а это человеческий щенок…\""
    )
    send(
      "$TREE_EMOJI Чтож. Вы можете помочь мне и иначе — у меня очень сильно чешется голова: " +
        "в листьях я своих запутался, которых аж 500. " +
        "Часть листьев у меня жадные: они любят отнимать — их у меня 300. " +
        "Часть листьев у меня предприимчивые: они любят делить, таких у меня 400. " +
        "Всякий лист либо жадный либо предприимчивый. " +
        "Понять мне надо бы, сколько листьев одновременно любят и отнимать, и делить — " +
        "они красного цвета уже, надо их сбрасывать. "
    )
    addIntegerReadHandler(
      200,
      this@L1S4,
      {
        sendImage("/leaving_forest.png")
        send(
          "$TREE_EMOJI: \"Листья молвят, что ты не ошибся. " +
            "Спасибо тебе за помощь, человеческое дитя! Теперь ты можешь идти дальше…\""
        )
        val userId = userId
        when (userId) {
          is StudentId -> L2S0Student(context, userId)
          is ParentId -> L2S0Parent(context, userId)
          else -> error("unreachable")
        }
      },
      {
        val userId = userId
        when (userId) {
          is StudentId -> L1S4WrongStudent(context, userId)
          is ParentId -> L1S4WrongParent(context, userId)
          else -> error("unreachable")
        }
      },
    )
  }
}

open class L1S4Wrong<ApiService : CommonUserApi<UserId>, UserId : CommonUserId>(
  override val context: User,
  override val userId: UserId,
) : QuestState<ApiService, UserId>() {
  override suspend fun BotContext.run(service: ApiService) {
    saveState(service)
    val buttons =
      listOf("$TREE_EMOJI Подойти к дубу", "\uD83E\uDD17 Почесать ещё раз", "\uD83D\uDD19  Назад")
    send(
        "$DOG_EMOJI Дуся: \"А пока ты отдыхаешь, может, почешешь мне пузо? Ну пожааалуйста! \uD83E\uDD7A\"\n",
        replyMarkup = verticalKeyboard(buttons),
      )
      .also { messagesWithKeyboard.add(it) }
    addDataCallbackHandler { callbackQuery ->
      when (callbackQuery.data) {
        buttons[0] -> {
          val userId = userId
          NewState(
            when (userId) {
              is StudentId -> L1S4Student(context, userId)
              is ParentId -> L1S4Parent(context, userId)
              else -> error("unreachable")
            }
          )
        }
        buttons[1] -> {
          val userId = userId
          NewState(
            when (userId) {
              is StudentId -> L1S4BellyrubStudent(context, userId)
              is ParentId -> L1S4BellyrubParent(context, userId)
              else -> error("unreachable")
            }
          )
        }
        buttons[2] -> {
          saveState(service)
          NewState(menuState())
        }
        else -> Unhandled
      }
    }
  }
}

open class L1S4Bellyrub<ApiService : CommonUserApi<UserId>, UserId : CommonUserId>(
  override val context: User,
  override val userId: UserId,
) : QuestState<ApiService, UserId>() {
  override suspend fun BotContext.run(service: ApiService) {
    saveState(service)
    sendImage("/bellyrub_2.png")
    val buttons = listOf("\uD83C\uDFDE Подойти к дубу")
    send(
        "Ах, да! Ты — самый лучший пузочесатель на свете! \uD83D\uDC3E",
        replyMarkup = verticalKeyboard(buttons),
      )
      .also { messagesWithKeyboard.add(it) }
    addDataCallbackHandler { callbackQuery ->
      when (callbackQuery.data) {
        buttons[0] -> {
          val userId = userId
          NewState(
            when (userId) {
              is StudentId -> L1S4Student(context, userId)
              is ParentId -> L1S4Parent(context, userId)
              else -> error("unreachable")
            }
          )
        }
        else -> Unhandled
      }
    }
  }
}

class L1S0Student(context: User, userId: StudentId) : L1S0<StudentApi, StudentId>(context, userId)

class L1S0Parent(context: User, userId: ParentId) : L1S0<ParentApi, ParentId>(context, userId)

class L1S1Student(context: User, userId: StudentId) : L1S1<StudentApi, StudentId>(context, userId)

class L1S1Parent(context: User, userId: ParentId) : L1S1<ParentApi, ParentId>(context, userId)

class L1S2Student(context: User, userId: StudentId) : L1S2<StudentApi, StudentId>(context, userId)

class L1S2Parent(context: User, userId: ParentId) : L1S2<ParentApi, ParentId>(context, userId)

class L1S3BellyrubStudent(context: User, userId: StudentId) :
  L1S3Bellyrub<StudentApi, StudentId>(context, userId)

class L1S3BellyrubParent(context: User, userId: ParentId) :
  L1S3Bellyrub<ParentApi, ParentId>(context, userId)

class L1S3Student(context: User, userId: StudentId) : L1S3<StudentApi, StudentId>(context, userId)

class L1S3Parent(context: User, userId: ParentId) : L1S3<ParentApi, ParentId>(context, userId)

class L1S4Student(context: User, userId: StudentId) : L1S4<StudentApi, StudentId>(context, userId)

class L1S4Parent(context: User, userId: ParentId) : L1S4<ParentApi, ParentId>(context, userId)

class L1S4WrongStudent(context: User, userId: StudentId) :
  L1S4Wrong<StudentApi, StudentId>(context, userId)

class L1S4WrongParent(context: User, userId: ParentId) :
  L1S4Wrong<ParentApi, ParentId>(context, userId)

class L1S4BellyrubStudent(context: User, userId: StudentId) :
  L1S4Bellyrub<StudentApi, StudentId>(context, userId)

class L1S4BellyrubParent(context: User, userId: ParentId) :
  L1S4Bellyrub<ParentApi, ParentId>(context, userId)
