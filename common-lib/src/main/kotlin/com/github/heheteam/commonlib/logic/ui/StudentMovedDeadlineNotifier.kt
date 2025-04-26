package com.github.heheteam.commonlib.logic.ui

import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.interfaces.SolutionId

interface StudentMovedDeadlineNotifier {
  suspend fun notifyStudentOnMovedDeadline(solutionId: SolutionId, assessment: SolutionAssessment)
}
