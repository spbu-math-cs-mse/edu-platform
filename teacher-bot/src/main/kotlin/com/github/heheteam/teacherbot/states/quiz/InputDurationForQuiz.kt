package com.github.heheteam.teacherbot.states.quiz

import com.github.heheteam.commonlib.api.TeacherApi
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.heheteam.commonlib.state.BotContext
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.getCurrentMoscowTime
import com.github.heheteam.commonlib.util.toNewState
import com.github.heheteam.teacherbot.states.SimpleTeacherState
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.types.chat.User
import korlibs.time.fromSeconds
import kotlin.time.Duration

private const val UPPER_BOUND = 100000

data class InputDurationForQuiz(
  override val context: User,
  override val userId: TeacherId,
  val metaInformationBuilder: QuizMetaInformationBuilder,
) : SimpleTeacherState() {
  override suspend fun BotContext.run(service: TeacherApi) {
    val upperBound = UPPER_BOUND
    send("Введите длительность в секундах теста от 0 до $upperBound").deleteLater()
    addTextMessageHandler { msg ->
      val durationSeconds = msg.content.text.toLongOrNull()
      if (durationSeconds != null && durationSeconds > 0 && durationSeconds < upperBound) {
        val quizMetaInfo =
          metaInformationBuilder
            .copy(duration = Duration.fromSeconds(durationSeconds))
            .toQuizMetaInformation(getCurrentMoscowTime())
        requireNotNull(quizMetaInfo) { "Failed to fill the bulder" }
        NewState(ConfirmQuiz(context, userId, quizMetaInfo))
      } else {
        send("Некорректный ввод. Попробуйте еще раз")
        this@InputDurationForQuiz.copy().toNewState()
      }
    }
  }
}
