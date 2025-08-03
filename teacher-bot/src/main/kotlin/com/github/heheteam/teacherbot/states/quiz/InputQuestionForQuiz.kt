package com.github.heheteam.teacherbot.states.quiz

import com.github.heheteam.commonlib.api.TeacherApi
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.heheteam.commonlib.state.BotContext
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.teacherbot.states.SimpleTeacherState
import dev.inmo.tgbotapi.types.chat.User

data class InputQuestionForQuiz(
  override val context: User,
  override val userId: TeacherId,
  val metaInformationBuilder: QuizMetaInformationBuilder,
) : SimpleTeacherState() {
  override suspend fun BotContext.run(service: TeacherApi) {
    send("Введите текст вопроса")
    addTextMessageHandler { msg ->
      val question = msg.content.text
      NewState(
        InputOptionsForQuiz(
          context,
          userId,
          metaInformationBuilder.copy(questionText = question),
          this@InputQuestionForQuiz.copy(),
        )
      )
    }
  }
}
