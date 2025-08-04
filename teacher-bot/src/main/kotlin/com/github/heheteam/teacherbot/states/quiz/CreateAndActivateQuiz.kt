package com.github.heheteam.teacherbot.states.quiz

import com.github.heheteam.commonlib.api.TeacherApi
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.heheteam.commonlib.quiz.QuizActivationResult
import com.github.heheteam.commonlib.quiz.QuizMetaInformation
import com.github.heheteam.commonlib.state.BotContext
import com.github.heheteam.commonlib.util.ButtonData
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.buildColumnMenu
import com.github.heheteam.commonlib.util.getCurrentInstant
import com.github.heheteam.commonlib.util.id
import com.github.heheteam.commonlib.util.map
import com.github.heheteam.teacherbot.states.SimpleTeacherState
import com.github.michaelbull.result.mapBoth
import dev.inmo.tgbotapi.types.chat.User

class CreateAndActivateQuiz(
  override val context: User,
  override val userId: TeacherId,
  val quizMetaInformation: QuizMetaInformation,
) : SimpleTeacherState() {
  override suspend fun BotContext.run(service: TeacherApi) {
    val result = service.createQuiz(quizMetaInformation).value
    val activateQuiz = service.activateQuiz(result, getCurrentInstant()).value
    val menu = buildColumnMenu(ButtonData("Меню", "menu", { defaultState() })).map(::NewState)
    when (activateQuiz) {
      QuizActivationResult.QuizAlreadyActive -> {
        send("Опрос уже активирован", replyMarkup = menu.keyboard).deleteLater()
      }
      QuizActivationResult.QuizNotFound -> {
        error("The quiz was just generated. How?")
      }
      QuizActivationResult.Success -> {
        send(
            "Опрос успешно отправлен. Вам придет уведомление в свое время",
            replyMarkup = menu.keyboard,
          )
          .deleteLater()
      }
    }
    addDataCallbackHandler {
      menu.handler(it.data).mapBoth(success = ::id, failure = { Unhandled })
    }
  }
}
