package com.github.heheteam.commonlib.logic

import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.Solution
import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.SolutionInputRequest
import com.github.heheteam.commonlib.interfaces.AssignmentId
import com.github.heheteam.commonlib.interfaces.ProblemGrade
import com.github.heheteam.commonlib.interfaces.ResponsibleTeacherResolver
import com.github.heheteam.commonlib.interfaces.SolutionId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.heheteam.commonlib.logic.ui.UiController
import com.github.heheteam.commonlib.notifications.BotEventBus
import com.github.michaelbull.result.binding
import kotlinx.datetime.LocalDateTime

internal class AcademicWorkflowService(
  private val academicWorkflowLogic: AcademicWorkflowLogic,
  private val responsibleTeacherResolver: ResponsibleTeacherResolver,
  private val botEventBus: BotEventBus,
  private val uiController: UiController,
) {
  fun sendSolution(solutionInputRequest: SolutionInputRequest) = binding {
    val teacher = responsibleTeacherResolver.resolveResponsibleTeacher(solutionInputRequest).bind()
    val solutionId = academicWorkflowLogic.inputSolution(solutionInputRequest, teacher)
    val solution =
      Solution(
        solutionId,
        solutionInputRequest.studentId,
        solutionInputRequest.telegramMessageInfo.chatId,
        solutionInputRequest.telegramMessageInfo.messageId,
        solutionInputRequest.problemId,
        solutionInputRequest.solutionContent,
        teacher,
        solutionInputRequest.timestamp,
      )
    botEventBus.publishNewSolutionEvent(solution)
    solutionId
  }

  fun assessSolution(
    solutionId: SolutionId,
    teacherId: TeacherId,
    assessment: SolutionAssessment,
    timestamp: LocalDateTime,
  ) {
    academicWorkflowLogic.assessSolution(solutionId, teacherId, assessment, timestamp)
    uiController.updateUiOnSolutionAssessment(solutionId, assessment)
  }

  fun getGradingsForAssignment(
    assignmentId: AssignmentId,
    studentId: StudentId,
  ): List<Pair<Problem, ProblemGrade>> {
    return academicWorkflowLogic.getGradingsForAssignment(assignmentId, studentId)
  }
}
