package com.github.heheteam.commonlib.quiz

import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.TeacherId
import kotlin.time.Duration
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime

private const val MAX_ANSWERS_IN_QUIZ = 10

data class QuizMetaInformation(
  val courseId: CourseId,
  val teacherId: TeacherId,
  val questionText: String,
  val answers: List<String>,
  val correctAnswerIndex: Int,
  val createdAt: LocalDateTime,
  val duration: Duration,
  val activationTime: Instant? = null,
) {
  init {
    require(answers.size >= 2) { "Quiz must have at least 2 answer options." }
    require(answers.size <= MAX_ANSWERS_IN_QUIZ) { "Quiz cannot have more than 10 answer options." }
    require(correctAnswerIndex in answers.indices) { "Correct answer index must be valid." }
  }
}
