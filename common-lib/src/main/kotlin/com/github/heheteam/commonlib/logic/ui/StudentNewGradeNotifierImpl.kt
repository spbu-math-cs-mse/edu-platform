package com.github.heheteam.commonlib.logic.ui

import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.interfaces.ProblemStorage
import com.github.heheteam.commonlib.interfaces.SolutionDistributor
import com.github.heheteam.commonlib.interfaces.SolutionId
import com.github.heheteam.commonlib.notifications.BotEventBus
import com.github.michaelbull.result.binding

internal class StudentNewGradeNotifierImpl(
  private val botEventBus: BotEventBus,
  private val problemStorage: ProblemStorage,
  private val solutionDistributor: SolutionDistributor,
) : StudentNewGradeNotifier {
  override fun notifyStudentOnNewAssessment(
    solutionId: SolutionId,
    assessment: SolutionAssessment,
  ) {
    binding {
      val solution = solutionDistributor.resolveSolution(solutionId).bind()
      val problem = problemStorage.resolveProblem(solution.problemId).bind()
      botEventBus.publishGradeEvent(
        solution.studentId,
        solution.chatId,
        solution.messageId,
        assessment,
        problem,
      )
    }
  }
}
