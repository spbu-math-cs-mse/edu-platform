package com.github.heheteam.commonlib.decorators

import com.github.heheteam.commonlib.ResolveError
import com.github.heheteam.commonlib.Solution
import com.github.heheteam.commonlib.SolutionContent
import com.github.heheteam.commonlib.SolutionInputRequest
import com.github.heheteam.commonlib.SolutionResolveError
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.ProblemId
import com.github.heheteam.commonlib.api.SolutionDistributor
import com.github.heheteam.commonlib.api.SolutionId
import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.googlesheets.GoogleSheetsRatingRecorder
import com.github.michaelbull.result.Result
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import java.time.LocalDateTime

class SolutionDistributorDecorator(
  private val solutionDistributor: SolutionDistributor,
  private val ratingRecorder: GoogleSheetsRatingRecorder,
) : SolutionDistributor {
  override fun inputSolution(
    studentId: StudentId,
    chatId: RawChatId,
    messageId: MessageId,
    solutionContent: SolutionContent,
    problemId: ProblemId,
    timestamp: LocalDateTime,
    teacherId: TeacherId?,
  ): SolutionId =
    solutionDistributor
      .inputSolution(studentId, chatId, messageId, solutionContent, problemId, timestamp, teacherId)
      .also { ratingRecorder.updateRating(problemId) }

  override fun querySolution(teacherId: TeacherId): Result<Solution?, SolutionResolveError> =
    solutionDistributor.querySolution(teacherId)

  override fun querySolution(courseId: CourseId): Result<Solution?, SolutionResolveError> =
    solutionDistributor.querySolution(courseId)

  override fun resolveSolution(solutionId: SolutionId): Result<Solution, ResolveError<SolutionId>> =
    solutionDistributor.resolveSolution(solutionId)

  override fun resolveSolutionCourse(
    solutionId: SolutionId
  ): Result<CourseId, ResolveError<SolutionId>> =
    solutionDistributor.resolveSolutionCourse(solutionId)

  override fun resolveResponsibleTeacher(solution: SolutionInputRequest): TeacherId? =
    solutionDistributor.resolveResponsibleTeacher(solution)

  override fun getSolutionsForProblem(problemId: ProblemId): List<SolutionId> =
    solutionDistributor.getSolutionsForProblem(problemId)

  override fun isSolutionAssessed(solutionId: SolutionId): Boolean =
    solutionDistributor.isSolutionAssessed(solutionId)
}
