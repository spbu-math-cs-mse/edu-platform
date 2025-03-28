package com.github.heheteam.teacherbot.logic

import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.api.GradeTable
import com.github.heheteam.commonlib.api.SolutionId
import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.logic.ui.UiController
import java.time.LocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime

/*
Grading entry consists of:
  student name;
  problem number
  assignmnent name

list of entries: each has:
  assessment
  teacher name
  timestamp

 */

/*
business tells us that on assessing solution we must:
- add a journal entry, which corresponds to our database thing
- send a student a notification that his solution was graded;
- update a record in a group related to the problem
- if the solution was sent from the group, not do things

 */

class SolutionGrader(val gradeTable: GradeTable, val uiController: UiController) {
  fun assessSolution(
    solutionId: SolutionId,
    teacherId: TeacherId,
    assessment: SolutionAssessment,
    timestamp: LocalDateTime,
  ) {
    gradeTable.recordSolutionAssessment(
      solutionId,
      teacherId,
      assessment,
      timestamp.toKotlinLocalDateTime(),
    )
    uiController.updateUiOnSolutionAssessment(solutionId, assessment)
  }
}
