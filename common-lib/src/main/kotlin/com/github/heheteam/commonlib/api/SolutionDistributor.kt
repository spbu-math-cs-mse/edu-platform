package com.github.heheteam.commonlib.api

import com.github.heheteam.commonlib.Solution
import com.github.heheteam.commonlib.SolutionContent
import com.github.michaelbull.result.Result
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import java.time.LocalDateTime

interface SolutionDistributor {
  @Suppress("LongParameterList")
  fun inputSolution(
    studentId: StudentId,
    chatId: RawChatId,
    messageId: MessageId,
    solutionContent: SolutionContent,
    problemId: ProblemId,
    timestamp: LocalDateTime = LocalDateTime.now(),
    teacherId: TeacherId? = null,
  ): SolutionId

  fun querySolution(teacherId: TeacherId): Result<Solution?, SolutionResolveError>

  fun resolveSolution(solutionId: SolutionId): Result<Solution, ResolveError<SolutionId>>

  fun getSolutionsForProblem(problemId: ProblemId): List<SolutionId>

  fun isSolutionAssessed(solutionId: SolutionId): Boolean
}
