package com.github.heheteam.teacherbot.states.quiz

import com.github.heheteam.commonlib.api.TeacherApi
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.heheteam.commonlib.quiz.QuizMetaInformation
import com.github.heheteam.commonlib.state.BotContext
import com.github.heheteam.commonlib.util.ButtonData
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.buildColumnMenu
import com.github.heheteam.commonlib.util.map
import com.github.heheteam.teacherbot.states.SimpleTeacherState
import dev.inmo.tgbotapi.types.chat.User

class ConfirmQuiz(
  override val context: User,
  override val userId: TeacherId,
  val quizMetaInfo: QuizMetaInformation,
) : SimpleTeacherState() {

  override suspend fun BotContext.run(service: TeacherApi) {
    val menu =
      buildColumnMenu(
          ButtonData("Да", "yes") { CreateAndActivateQuiz(context, userId, quizMetaInfo) },
          ButtonData("Нет", "menu", { defaultState() }),
        )
        .map(::NewState)
    send(
        "Вы выбрали правильным ответом \"${quizMetaInfo.answers.getOrNull(quizMetaInfo.correctAnswerIndex)}\""
      )
      .deleteLater()
    send(
        "Подтвердить отправку сообщения? С нажатием кнопки \"Да\" опрос разошлется",
        replyMarkup = menu.keyboard,
      )
      .deleteLater()
    registerStateMenu(menu)
  }
}
