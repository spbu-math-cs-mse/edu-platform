package com.github.heheteam.teacherbot.logic

import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.api.SolutionId

class StudentNewGradeNotifierImpl : StudentNewGradeNotifier {
  override fun notifyStudentOnNewAssignment(solutionId: SolutionId, good: SolutionAssessment) = Unit
}
