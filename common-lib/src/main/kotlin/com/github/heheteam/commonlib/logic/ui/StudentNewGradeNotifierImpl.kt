package com.github.heheteam.commonlib.logic.ui

import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.interfaces.ProblemStorage
import com.github.heheteam.commonlib.interfaces.SolutionDistributor
import com.github.heheteam.commonlib.interfaces.SolutionId
import com.github.heheteam.commonlib.telegram.StudentBotTelegramController
import com.github.michaelbull.result.coroutines.coroutineBinding

internal class StudentNewGradeNotifierImpl(
  private val studentBotTelegramController: StudentBotTelegramController,
  private val problemStorage: ProblemStorage,
  private val solutionDistributor: SolutionDistributor,
) : StudentNewGradeNotifier {
  override suspend fun notifyStudentOnNewAssessment(
    solutionId: SolutionId,
    assessment: SolutionAssessment,
  ) {
    coroutineBinding {
      val solution = solutionDistributor.resolveSolution(solutionId).bind()
      val problem = problemStorage.resolveProblem(solution.problemId).bind()

      studentBotTelegramController.notifyStudentOnNewAssessment(
        solution.chatId,
        solution.messageId,
        solution.studentId,
        problem,
        assessment,
      )
    }
  }
}
