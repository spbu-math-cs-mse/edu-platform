package com.github.heheteam.teacherbot.logic

import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.api.SolutionId

interface UiController {
  fun updateUiOnSolutionAssessment(solutionId: SolutionId, good: SolutionAssessment)
}
