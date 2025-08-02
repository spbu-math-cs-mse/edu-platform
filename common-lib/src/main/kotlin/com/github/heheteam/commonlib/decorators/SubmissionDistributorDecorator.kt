package com.github.heheteam.commonlib.decorators

import com.github.heheteam.commonlib.Submission
import com.github.heheteam.commonlib.SubmissionInputRequest
import com.github.heheteam.commonlib.TextWithMediaAttachments
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.heheteam.commonlib.errors.ResolveError
import com.github.heheteam.commonlib.errors.SubmissionResolveError
import com.github.heheteam.commonlib.googlesheets.GoogleSheetsRatingRecorder
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.ProblemId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.SubmissionDistributor
import com.github.heheteam.commonlib.interfaces.SubmissionId
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.michaelbull.result.Result
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId

internal class SubmissionDistributorDecorator(
  private val submissionDistributor: SubmissionDistributor,
  private val ratingRecorder: GoogleSheetsRatingRecorder,
) : SubmissionDistributor {
  override fun inputSubmission(
    studentId: StudentId,
    chatId: RawChatId,
    messageId: MessageId,
    submissionContent: TextWithMediaAttachments,
    problemId: ProblemId,
    timestamp: kotlinx.datetime.LocalDateTime,
    teacherId: TeacherId?,
  ): SubmissionId =
    submissionDistributor
      .inputSubmission(
        studentId,
        chatId,
        messageId,
        submissionContent,
        problemId,
        timestamp,
        teacherId,
      )
      .also { ratingRecorder.updateRating(problemId) }

  override fun querySubmission(teacherId: TeacherId): Result<Submission?, SubmissionResolveError> =
    submissionDistributor.querySubmission(teacherId)

  override fun querySubmission(courseId: CourseId): Result<Submission?, SubmissionResolveError> =
    submissionDistributor.querySubmission(courseId)

  override fun resolveSubmission(
    submissionId: SubmissionId
  ): Result<Submission, ResolveError<SubmissionId>> =
    submissionDistributor.resolveSubmission(submissionId)

  override fun resolveSubmissionCourse(
    submissionId: SubmissionId
  ): Result<CourseId, ResolveError<SubmissionId>> =
    submissionDistributor.resolveSubmissionCourse(submissionId)

  override fun resolveResponsibleTeacher(
    submissionInputRequest: SubmissionInputRequest
  ): TeacherId? = submissionDistributor.resolveResponsibleTeacher(submissionInputRequest)

  override fun getSubmissionsForProblem(
    problemId: ProblemId
  ): Result<List<SubmissionId>, EduPlatformError> =
    submissionDistributor.getSubmissionsForProblem(problemId)

  override fun isSubmissionAssessed(submissionId: SubmissionId): Result<Boolean, EduPlatformError> =
    submissionDistributor.isSubmissionAssessed(submissionId)
}
