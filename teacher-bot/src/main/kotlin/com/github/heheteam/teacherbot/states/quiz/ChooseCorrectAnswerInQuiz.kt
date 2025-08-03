package com.github.heheteam.teacherbot.states.quiz

import com.github.heheteam.commonlib.api.TeacherApi
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.heheteam.commonlib.state.BotContext
import com.github.heheteam.commonlib.util.ButtonData
import com.github.heheteam.commonlib.util.toColumnMenu
import com.github.heheteam.commonlib.util.toNewState
import com.github.heheteam.teacherbot.states.SimpleTeacherState
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.types.chat.User

class ChooseCorrectAnswerInQuiz(
  override val context: User,
  override val userId: TeacherId,
  val metaInformationBuilder: QuizMetaInformationBuilder,
  val previousState: State?,
) : SimpleTeacherState() {
  override suspend fun BotContext.run(service: TeacherApi) {
    val message =
      metaInformationBuilder.questionText ?: throw IllegalArgumentException("null message")
    val options = metaInformationBuilder.answers ?: throw IllegalArgumentException("null  options")
    send("Нажмите на правильный ответ. Обратите внимание, именно так будет видеть ваш текст ученик")
    val menu =
      options
        .mapIndexed { index, option ->
          ButtonData(option, index.toString()) { inputIndexAndContinue(index).toNewState() }
        }
        .toColumnMenu()
    send(message, replyMarkup = menu.keyboard)
    registerStateMenu(menu)
  }

  private fun inputIndexAndContinue(index: Int): InputDurationForQuiz =
    InputDurationForQuiz(context, userId, metaInformationBuilder.copy(correctAnswerIndex = index))
}
