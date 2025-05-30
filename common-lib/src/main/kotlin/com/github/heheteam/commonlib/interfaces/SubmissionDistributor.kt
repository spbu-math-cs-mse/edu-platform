package com.github.heheteam.commonlib.interfaces

import com.github.heheteam.commonlib.ResolveError
import com.github.heheteam.commonlib.Submission
import com.github.heheteam.commonlib.SubmissionInputRequest
import com.github.heheteam.commonlib.SubmissionResolveError
import com.github.heheteam.commonlib.TextWithMediaAttachments
import com.github.michaelbull.result.Result
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import java.time.LocalDateTime

internal interface SubmissionDistributor {
  @Suppress("LongParameterList")
  fun inputSubmission(
    studentId: StudentId,
    chatId: RawChatId,
    messageId: MessageId,
    submissionContent: TextWithMediaAttachments,
    problemId: ProblemId,
    timestamp: LocalDateTime = LocalDateTime.now(),
    teacherId: TeacherId? = null,
  ): SubmissionId

  fun querySubmission(teacherId: TeacherId): Result<Submission?, SubmissionResolveError>

  fun querySubmission(courseId: CourseId): Result<Submission?, SubmissionResolveError>

  fun resolveSubmission(submissionId: SubmissionId): Result<Submission, ResolveError<SubmissionId>>

  fun resolveSubmissionCourse(
    submissionId: SubmissionId
  ): Result<CourseId, ResolveError<SubmissionId>>

  fun resolveResponsibleTeacher(submissionInputRequest: SubmissionInputRequest): TeacherId?

  fun getSubmissionsForProblem(problemId: ProblemId): List<SubmissionId>

  fun isSubmissionAssessed(submissionId: SubmissionId): Boolean
}
