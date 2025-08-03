package com.github.heheteam.teacherbot.states.quiz

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.heheteam.commonlib.quiz.QuizMetaInformation
import kotlin.time.Duration
import kotlinx.datetime.LocalDateTime

data class QuizMetaInformationBuilder(
  val course: Course,
  val teacherId: TeacherId,
  val questionText: String? = null,
  val answers: List<String>? = null,
  val correctAnswerIndex: Int? = null,
  val duration: Duration? = null,
) {
  @Suppress("ReturnCount")
  fun toQuizMetaInformation(createdTime: LocalDateTime): QuizMetaInformation? {
    return QuizMetaInformation(
      course.id,
      teacherId,
      questionText ?: return null,
      answers ?: return null,
      correctAnswerIndex ?: return null,
      createdTime,
      duration ?: return null,
    )
  }
}
