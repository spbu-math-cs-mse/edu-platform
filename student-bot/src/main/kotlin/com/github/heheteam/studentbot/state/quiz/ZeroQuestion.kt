package com.github.heheteam.studentbot.state.quiz

import com.github.heheteam.commonlib.TextWithMediaAttachments
import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.state.MultipleChoiceQuestionBotState
import com.github.heheteam.studentbot.state.MenuState
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.utils.buildEntities
import dev.inmo.tgbotapi.utils.link

class ZeroQuestion(context: User, userId: StudentId) :
  MultipleChoiceQuestionBotState<StudentApi, StudentId>(context, userId) {

  override val question: TextWithMediaAttachments =
    TextWithMediaAttachments(
      text =
        buildEntities(System.lineSeparator()) {
          +"Ррраф! Привет, я Такса Дуся, я помогаю учить математику, но сейчас лето и я просто чиллю."
          +"Говорят, кто-то в городе приготовил индейку, а я очень ее люблю! Поможешь мне достать ее?"
        }
    )

  override val correctAnswer: String = "Да"
  override val incorrectAnswers = listOf("Нет, кто не работает, тот не ест!")

  override val correctAnswerReply: TextWithMediaAttachments =
    TextWithMediaAttachments(
      text =
        buildEntities {
          +"Прежде всего нам придется преодалеть лабринт, избегая рраааф, шпитца --- я их не любли!"
        }
    )
  override val incorrectAnswerReply: TextWithMediaAttachments =
    TextWithMediaAttachments(
      text =
        buildEntities(System.lineSeparator()) {
          +"Эх, с тобой не поволонишь!( Заходи тогда нам ботать на курс"
          link("https://dabromat.ru/start")
        }
    )

  override val nextState = { user: User, studentId: StudentId -> FirstQuestion(user, studentId) }
  override val menuState = { user: User, studentId: StudentId -> MenuState(user, studentId) }
}
