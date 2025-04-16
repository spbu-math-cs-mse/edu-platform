package com.github.heheteam.commonlib.logic.ui

import com.github.heheteam.commonlib.interfaces.SolutionId

interface JournalUpdater {
  suspend fun updateJournalDisplaysForSolution(solutionId: SolutionId)
}
