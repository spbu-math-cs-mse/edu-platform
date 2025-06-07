package com.github.heheteam.commonlib.logic

import com.github.heheteam.commonlib.EduPlatformError
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.Submission
import com.github.heheteam.commonlib.SubmissionAssessment
import com.github.heheteam.commonlib.SubmissionInputRequest
import com.github.heheteam.commonlib.interfaces.AssignmentId
import com.github.heheteam.commonlib.interfaces.ProblemGrade
import com.github.heheteam.commonlib.interfaces.ResponsibleTeacherResolver
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.SubmissionId
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.heheteam.commonlib.logic.ui.NewSubmissionNotificationStatus
import com.github.heheteam.commonlib.logic.ui.NewSubmissionTeacherNotifier
import com.github.heheteam.commonlib.logic.ui.UiController
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.mapBoth
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime

sealed interface SubmissionSendingResult {
  data class Success(
    val submissionId: SubmissionId,
    val newSubmissionNotificationStatus: NewSubmissionNotificationStatus,
  ) : SubmissionSendingResult

  data class Failure(val error: EduPlatformError) : SubmissionSendingResult
}

internal class AcademicWorkflowService(
  private val academicWorkflowLogic: AcademicWorkflowLogic,
  private val responsibleTeacherResolver: ResponsibleTeacherResolver,
  private val uiController: UiController,
  private val newSubmissionTeacherNotifier: NewSubmissionTeacherNotifier,
) {
  fun sendSubmission(submissionInputRequest: SubmissionInputRequest): SubmissionSendingResult {
    val maybeSubmissionIdAndTeacher = binding {
      val teacher =
        responsibleTeacherResolver.resolveResponsibleTeacher(submissionInputRequest).bind()
      val submissionId = academicWorkflowLogic.inputSubmission(submissionInputRequest, teacher)
      submissionId to teacher
    }
    return maybeSubmissionIdAndTeacher.mapBoth(
      success = { (submissionId, teacher) ->
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
        val notificationStatus = runBlocking {
          newSubmissionTeacherNotifier.notifyNewSubmission(submission)
        }
        SubmissionSendingResult.Success(submissionId, notificationStatus)
      },
      failure = { SubmissionSendingResult.Failure(it) },
    )
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
  ): Result<List<Pair<Problem, ProblemGrade>>, EduPlatformError> {
    return academicWorkflowLogic.getGradingsForAssignment(assignmentId, studentId)
  }
}
