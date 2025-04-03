package com.github.heheteam.teacherbot.states

import com.github.heheteam.commonlib.Grade
import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.api.SolutionId
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage

sealed interface TeacherAction

data class GradingFromButton(val solutionId: SolutionId, val grade: Grade) : TeacherAction

data class GradingFromReply(
  val solutionId: SolutionId,
  val solutionAssessment: SolutionAssessment,
) : TeacherAction

data class ConfirmSending(val solutionId: SolutionId, val solutionAssessment: SolutionAssessment) :
  TeacherAction

data class DeleteMessage(val message: AccessibleMessage?) : TeacherAction
