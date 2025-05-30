package com.github.heheteam.commonlib.logic.ui

import com.github.heheteam.commonlib.SubmissionAssessment
import com.github.heheteam.commonlib.interfaces.SubmissionId

interface StudentNewGradeNotifier {
  suspend fun notifyStudentOnNewAssessment(
    submissionId: SubmissionId,
    assessment: SubmissionAssessment,
  )
}
