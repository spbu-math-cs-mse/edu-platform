package com.github.heheteam.commonlib.logic

import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.SolutionInputRequest
import com.github.heheteam.commonlib.api.GradeTable
import com.github.heheteam.commonlib.api.SolutionDistributor
import com.github.heheteam.commonlib.api.SolutionId
import com.github.heheteam.commonlib.api.TeacherId
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime

class AcademicWorkflowLogic(
  private val solutionDistributor: SolutionDistributor,
  private val gradeTable: GradeTable,
) {
  fun inputSolution(
    solutionInputRequest: SolutionInputRequest,
    responsibleTeacher: TeacherId,
  ): SolutionId {
    return solutionDistributor.inputSolution(
      solutionInputRequest.studentId,
      solutionInputRequest.telegramMessageInfo.chatId,
      solutionInputRequest.telegramMessageInfo.messageId,
      solutionInputRequest.solutionContent,
      solutionInputRequest.problemId,
      solutionInputRequest.timestamp.toJavaLocalDateTime(),
      responsibleTeacher,
    )
  }

  fun assessSolution(
    solutionId: SolutionId,
    teacherId: TeacherId,
    assessment: SolutionAssessment,
    timestamp: LocalDateTime,
  ) {
    gradeTable.recordSolutionAssessment(solutionId, teacherId, assessment, timestamp)
  }
}
