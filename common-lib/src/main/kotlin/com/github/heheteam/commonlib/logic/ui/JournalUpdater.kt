package com.github.heheteam.commonlib.logic.ui

import com.github.heheteam.commonlib.api.SolutionId

interface JournalUpdater {
  fun updateJournalDisplaysForSolution(solutionId: SolutionId)
}
