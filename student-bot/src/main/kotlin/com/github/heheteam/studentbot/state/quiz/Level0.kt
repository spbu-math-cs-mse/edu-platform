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

const val DOG_EMOJI = "\uD83D\uDC36"

open class L0<ApiService : CommonUserApi<UserId>, UserId : CommonUserId>(
  override val context: User,
  override val userId: UserId,
) : QuestState<ApiService, UserId>() {
  override suspend fun BotContext.run(service: ApiService) {
    saveState(service)
    sendImage("/forest.png")
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
        buttons[0] -> {
          val userId = userId
          NewState(
            when (userId) {
              is StudentId -> L1S0Student(context, userId)
              is ParentId -> L1S0Parent(context, userId)
              else -> error("unreachable")
            }
          )
        }
        buttons[1] -> {
          NewState(menuState())
        }
        else -> Unhandled
      }
    }
  }
}

class L0Student(context: User, userId: StudentId) : L0<StudentApi, StudentId>(context, userId)

class L0Parent(context: User, userId: ParentId) : L0<ParentApi, ParentId>(context, userId)
