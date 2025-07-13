package com.github.heheteam.studentbot.state.quiz

import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.studentbot.state.MenuState
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.urlButton
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.utils.row

private const val DABROMAT_URL = "https://dabromat.ru/start"

class LFS0(override val context: User, override val userId: StudentId) : QuestState() {
  override suspend fun BotContext.run() {
    send(
      "ТЫ НА ВЕРШИНЕ!\n Перед тобой — сияющая УПАВШАЯ ЗВЕЗДА.\n" +
        "Она пульсирует светом, будто знает: теперь ты — настоящий математик."
    )
    val buttons = listOf("\uD83C\uDFC5 Получить Сертификат", "\uD83C\uDF93 Узнать о курсе", "\uD83D\uDD19 В меню!")
    send(
      "\uD83D\uDC36 Дуся: \"Ты прошёл весь путь! А хочешь и дальше так учиться? " +
        "У нас в Дабромате есть курс, где всё вот так же — весело и умно!\"\n",
      replyMarkup = inlineKeyboard {
        row { dataButton(buttons[0], buttons[0]) }
        row { urlButton(buttons[1], DABROMAT_URL) }
        row { dataButton(buttons[2], buttons[2]) }
      }
    ).also { messagesWithKeyboard.add(it) }
    addDataCallbackHandler { callbackQuery ->
      when (callbackQuery.data) {
        buttons[0] -> NewState(LFS1(context, userId))
        buttons[2] -> NewState(MenuState(context, userId))
        else -> Unhandled
      }
    }
  }
}


class LFS1(override val context: User, override val userId: StudentId) : QuestState() {
  override suspend fun BotContext.run() {
    send("Поздравляем! Ты получаешь Сертификат Героя Матемаланда \uD83C\uDFC6\n")
    val buttons = listOf("\uD83C\uDF93 Узнать о курсе\n", "\uD83D\uDD19 В меню!")
    send(
      "\uD83D\uDC36 Дуся: \"Покажи его родителям! А я расскажу им, как ты можешь учиться дальше.\"",
      replyMarkup = inlineKeyboard {
        row { urlButton(buttons[0], DABROMAT_URL) }
        row { dataButton(buttons[1], buttons[1]) }
      }
    ).also { messagesWithKeyboard.add(it) }
    addDataCallbackHandler { callbackQuery ->
      when (callbackQuery.data) {
        buttons[1] -> NewState(MenuState(context, userId))
        else -> Unhandled
      }
    }
  }
}
