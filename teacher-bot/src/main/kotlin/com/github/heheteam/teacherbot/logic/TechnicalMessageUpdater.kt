package com.github.heheteam.teacherbot.logic

import com.github.heheteam.commonlib.api.GradingEntry
import com.github.heheteam.commonlib.api.SolutionId

interface TechnicalMessageUpdater {
  fun updateTechnicalMessageInGroup(solutionId: SolutionId, gradings: List<GradingEntry>)
}
