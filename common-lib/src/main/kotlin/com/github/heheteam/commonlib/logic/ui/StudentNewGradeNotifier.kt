package com.github.heheteam.commonlib.logic.ui

import com.github.heheteam.commonlib.EduPlatformError
import com.github.heheteam.commonlib.SubmissionAssessment
import com.github.heheteam.commonlib.interfaces.SubmissionId
import com.github.michaelbull.result.Result

interface StudentNewGradeNotifier {
  suspend fun notifyStudentOnNewAssessment(
    submissionId: SubmissionId,
    assessment: SubmissionAssessment,
  ): Result<Unit, EduPlatformError>
}
