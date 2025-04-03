package com.github.heheteam.commonlib.logic.ui

import com.github.heheteam.commonlib.interfaces.GradingEntry
import com.github.heheteam.commonlib.interfaces.SolutionId

interface SolutionMessageUpdater {
  fun updateSolutionMessageInGroup(solutionId: SolutionId, gradings: List<GradingEntry>)

  fun updateSolutionMessageInPersonalChat(solutionId: SolutionId, gradings: List<GradingEntry>)
}
