package com.github.heheteam.teacherbot.logic

import com.github.heheteam.commonlib.Solution
import com.github.heheteam.commonlib.api.GradingEntry
import com.github.heheteam.commonlib.api.SolutionId

interface SolutionMessageUpdater {
  fun updateSolutionMessageInGroup(solutionId: SolutionId, gradings: List<GradingEntry>)

  fun updateSolutionMessageInPersonalChat(solutionId: SolutionId, gradings: List<GradingEntry>)
}

interface MenuMessageUpdater {
  fun updateMenuMessageInPersonalChat(solutionId: SolutionId)
}
