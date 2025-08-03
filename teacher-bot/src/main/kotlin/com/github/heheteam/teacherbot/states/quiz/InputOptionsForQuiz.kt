package com.github.heheteam.teacherbot.states.quiz

import com.github.heheteam.commonlib.api.TeacherApi
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.heheteam.commonlib.state.BotContext
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.teacherbot.states.SimpleTeacherState
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.types.chat.User

data class InputOptionsForQuiz(
  override val context: User,
  override val userId: TeacherId,
  val metaInformationBuilder: QuizMetaInformationBuilder,
  val previousState: State?,
) : SimpleTeacherState() {
  override suspend fun BotContext.run(service: TeacherApi) {
    send("Введите опции для создания опроса")
    addTextMessageHandler { msg ->
      val options = msg.content.text.split("\n")
      NewState(
        ChooseCorrectAnswerInQuiz(
          context,
          userId,
          metaInformationBuilder.copy(answers = options),
          this@InputOptionsForQuiz.copy(),
        )
      )
    }
  }
}
