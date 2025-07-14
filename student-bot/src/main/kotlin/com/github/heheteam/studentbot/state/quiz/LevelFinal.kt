package com.github.heheteam.studentbot.state.quiz

import com.github.heheteam.commonlib.api.CommonUserApi
import com.github.heheteam.commonlib.interfaces.CommonUserId
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.urlButton
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.utils.row

private const val DABROMAT_URL = "https://dabromat.ru/start"

class L4Final<ApiService : CommonUserApi<UserId>, UserId : CommonUserId>(
  override val context: User,
  override val userId: UserId,
) : QuestState<ApiService, UserId>() {
  override suspend fun BotContext.run(service: ApiService) {
    sendImage("/star.png")
    send("Ты стоишь на вершине. Перед тобой — сияющая звезда.\n")
    send(
      "$DOG_EMOJI Дуся: \"Это — Звезда Дабромат. Она ждала тебя. " +
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
      replyMarkup =
        inlineKeyboard {
          row { dataButton(buttons[0], buttons[0]) }
          row { urlButton(buttons[1], DABROMAT_URL) }
        },
    )

    addDataCallbackHandler { callbackQuery ->
      when (callbackQuery.data) {
        buttons[0] -> NewState(L4Certificate(context, userId))
        // TODO: buttons[1] -> NewState(TODO())
        else -> Unhandled
      }
    }
  }
}

class L4Certificate<ApiService : CommonUserApi<UserId>, UserId : CommonUserId>(
  override val context: User,
  override val userId: UserId,
) : QuestState<ApiService, UserId>() {
  override suspend fun BotContext.run(service: ApiService) {
    sendMarkdown("Поздравляем! Ты получаешь *Сертификат Героя Матемаланда* \uD83C\uDFC6\n")
    val buttons = listOf("\uD83C\uDF93 Узнать о курсе\n", "\uD83D\uDD19 В меню!")
    send(
        "$DOG_EMOJI Дуся: \"Покажи его родителям! А я расскажу им, как ты можешь учиться дальше.\"",
        replyMarkup =
          inlineKeyboard {
            row { urlButton(buttons[0], DABROMAT_URL) }
            row { dataButton(buttons[1], buttons[1]) }
          },
      )
      .also { messagesWithKeyboard.add(it) }
    addDataCallbackHandler { callbackQuery ->
      when (callbackQuery.data) {
        buttons[1] -> {
          saveState(service)
          NewState(menuState())
        }
        else -> Unhandled
      }
    }
  }
}
