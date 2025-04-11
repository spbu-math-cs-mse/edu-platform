package com.github.heheteam.commonlib.interfaces

import com.github.heheteam.commonlib.ResolveError
import com.github.heheteam.commonlib.Solution
import com.github.heheteam.commonlib.SolutionInputRequest
import com.github.heheteam.commonlib.SolutionResolveError
import com.github.heheteam.commonlib.TextWithMediaAttachments
import com.github.michaelbull.result.Result
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import java.time.LocalDateTime

internal interface SolutionDistributor {
  @Suppress("LongParameterList")
  fun inputSolution(
    studentId: StudentId,
    chatId: RawChatId,
    messageId: MessageId,
    solutionContent: TextWithMediaAttachments,
    problemId: ProblemId,
    timestamp: LocalDateTime = LocalDateTime.now(),
    teacherId: TeacherId? = null,
  ): SolutionId

  fun querySolution(teacherId: TeacherId): Result<Solution?, SolutionResolveError>

  fun querySolution(courseId: CourseId): Result<Solution?, SolutionResolveError>

  fun resolveSolution(solutionId: SolutionId): Result<Solution, ResolveError<SolutionId>>

  fun resolveSolutionCourse(solutionId: SolutionId): Result<CourseId, ResolveError<SolutionId>>

  fun resolveResponsibleTeacher(solution: SolutionInputRequest): TeacherId?

  fun getSolutionsForProblem(problemId: ProblemId): List<SolutionId>

  fun isSolutionAssessed(solutionId: SolutionId): Boolean
}
