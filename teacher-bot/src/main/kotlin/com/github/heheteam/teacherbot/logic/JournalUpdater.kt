package com.github.heheteam.teacherbot.logic

import com.github.heheteam.commonlib.api.SolutionId

interface JournalUpdater {
  fun updateJournalDisplaysForSolution(solutionId: SolutionId)
}
