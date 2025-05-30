package com.github.heheteam.commonlib.logic

import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.Submission
import com.github.heheteam.commonlib.SubmissionAssessment
import com.github.heheteam.commonlib.SubmissionInputRequest
import com.github.heheteam.commonlib.TeacherResolveError
import com.github.heheteam.commonlib.interfaces.AssignmentId
import com.github.heheteam.commonlib.interfaces.ProblemGrade
import com.github.heheteam.commonlib.interfaces.ResponsibleTeacherResolver
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.SubmissionId
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.heheteam.commonlib.logic.ui.UiController
import com.github.heheteam.commonlib.notifications.BotEventBus
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import kotlinx.datetime.LocalDateTime

internal class AcademicWorkflowService(
  private val academicWorkflowLogic: AcademicWorkflowLogic,
  private val responsibleTeacherResolver: ResponsibleTeacherResolver,
  private val botEventBus: BotEventBus,
  private val uiController: UiController,
) {
  fun sendSubmission(
    submissionInputRequest: SubmissionInputRequest
  ): Result<SubmissionId, TeacherResolveError> = binding {
    val teacher =
      responsibleTeacherResolver.resolveResponsibleTeacher(submissionInputRequest).bind()
    val submissionId = academicWorkflowLogic.inputSubmission(submissionInputRequest, teacher)
    val submission =
      Submission(
        submissionId,
        submissionInputRequest.studentId,
        submissionInputRequest.telegramMessageInfo.chatId,
        submissionInputRequest.telegramMessageInfo.messageId,
        submissionInputRequest.problemId,
        submissionInputRequest.submissionContent,
        teacher,
        submissionInputRequest.timestamp,
      )
    botEventBus.publishNewSubmissionEvent(submission)
    submissionId
  }

  fun assessSubmission(
    submissionId: SubmissionId,
    teacherId: TeacherId,
    assessment: SubmissionAssessment,
    timestamp: LocalDateTime,
  ) {
    academicWorkflowLogic.assessSubmission(submissionId, teacherId, assessment, timestamp)
    uiController.updateUiOnSubmissionAssessment(submissionId, assessment)
  }

  fun getGradingsForAssignment(
    assignmentId: AssignmentId,
    studentId: StudentId,
  ): List<Pair<Problem, ProblemGrade>> {
    return academicWorkflowLogic.getGradingsForAssignment(assignmentId, studentId)
  }
}
