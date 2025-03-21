package com.github.heheteam.teacherbot.states

import com.github.heheteam.commonlib.Grade
import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.api.SolutionId

sealed interface TeacherAction

data class GradingFromButton(val solutionId: SolutionId, val grade: Grade) : TeacherAction

data class GradingFromReply(
  val solutionId: SolutionId,
  val solutionAssessment: SolutionAssessment,
) : TeacherAction

data class ConfirmSending(val solutionId: SolutionId, val solutionAssessment: SolutionAssessment) :
  TeacherAction
