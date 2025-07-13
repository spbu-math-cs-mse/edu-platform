package com.github.heheteam.studentbot.state.quiz

import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.studentbot.state.MenuState
import dev.inmo.tgbotapi.types.chat.User

const val DOG_EMOJI = "\uD83D\uDC36"

class L0(override val context: User, override val userId: StudentId) : QuestState() {
  override suspend fun BotContext.run() {
    sendImage("/forest.tiff")
    send(
      "\uD83C\uDF0C Дуся идёт рядом с тобой по лесной тропинке. " +
        "Листья шуршат, деревья склоняются, будто подслушивают."
    )
    sendMarkdown(
      "$DOG_EMOJI Дуся: \"Давным-давно над Матемаландом пролетела *волшебная звезда*. " +
        "Она упала сюда... прямо на вершину *Мозговой Горы*. " +
        "Говорят, в ней — древняя мудрость: она открывает разум и помогает тем, кто ищет знания.\"\n"
    )
    send(
      "Но добраться до неё не просто. Путь долгий: леса, лабиринты, загадки… " +
        "Если мы пройдём всё — сможем прикоснуться к ней и стать по-настоящему сильными в математике."
    )
    val buttons = listOf("\uD83D\uDE80 Конечно, Дуся!", "\uD83D\uDD19 Назад\n")
    send("Ты со мной? \uD83D\uDC3E\n", replyMarkup = verticalKeyboard(buttons)).also {
      messagesWithKeyboard.add(it)
    }
    addDataCallbackHandler { callbackQuery ->
      when (callbackQuery.data) {
        buttons[0] -> NewState(L1S0(context, userId))
        buttons[1] -> NewState(MenuState(context, userId))
        else -> Unhandled
      }
    }
  }
}
