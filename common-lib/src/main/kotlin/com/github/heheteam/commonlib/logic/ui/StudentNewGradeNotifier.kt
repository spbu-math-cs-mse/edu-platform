package com.github.heheteam.commonlib.logic.ui

import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.api.SolutionId

interface StudentNewGradeNotifier {
  fun notifyStudentOnNewAssessment(solutionId: SolutionId, assessment: SolutionAssessment)
}
