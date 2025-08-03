package com.github.heheteam.commonlib.quiz

import com.github.heheteam.commonlib.interfaces.QuizId
import com.github.heheteam.commonlib.interfaces.StudentId
import kotlinx.datetime.Instant

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
}
