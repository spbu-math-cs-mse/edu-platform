package com.github.heheteam.commonlib.quiz

import com.github.heheteam.commonlib.interfaces.QuizId
import com.github.heheteam.commonlib.interfaces.StudentId
import kotlinx.datetime.Instant

data class QuizOverallResults(
  val totalParticipants: Int,
  val correctAnswers: Int,
  val incorrectAnswers: Int,
  val notAnswered: Int,
)

class RichQuiz(
  val id: QuizId,
  val metaInformation: QuizMetaInformation,
  var isActive: Boolean,
  val studentAnswers: Map<StudentId, Int> = emptyMap(),
) {
  fun tryDeactivate(currentTime: Instant): QuizDeactivationStatus {
    val activationTime = metaInformation.activationTime
    return if (
      isActive && activationTime != null && currentTime >= activationTime + metaInformation.duration
    ) {
      isActive = false
      QuizDeactivationStatus.Deactivated
    } else {
      QuizDeactivationStatus.NotDeactivated
    }
  }

  fun getStudentPerformance(): Map<StudentId, Int> {
    return studentAnswers
  }

  fun getQuizOverallResults(allStudentsInCourse: List<StudentId>): QuizOverallResults {
    var correctAnswers = 0
    var incorrectAnswers = 0
    val answeredStudentIds = mutableSetOf<StudentId>()

    for ((studentId, chosenAnswerIndex) in studentAnswers) {
      answeredStudentIds.add(studentId)
      if (chosenAnswerIndex == metaInformation.correctAnswerIndex) {
        correctAnswers++
      } else {
        incorrectAnswers++
      }
    }

    val notAnswered = allStudentsInCourse.size - answeredStudentIds.size

    return QuizOverallResults(
      totalParticipants = allStudentsInCourse.size,
      correctAnswers = correctAnswers,
      incorrectAnswers = incorrectAnswers,
      notAnswered = notAnswered,
    )
  }
}
