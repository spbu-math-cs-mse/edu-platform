package com.github.heheteam.commonlib.logic.ui

import com.github.heheteam.commonlib.SubmissionAssessment
import com.github.heheteam.commonlib.interfaces.SubmissionId

interface UiController {
  fun updateUiOnSubmissionAssessment(submissionId: SubmissionId, assessment: SubmissionAssessment)
}
