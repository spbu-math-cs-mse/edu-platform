package com.github.heheteam.teacherbot.logic

import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.api.BotEventBus
import com.github.heheteam.commonlib.api.ProblemStorage
import com.github.heheteam.commonlib.api.SolutionDistributor
import com.github.heheteam.commonlib.api.SolutionId
import com.github.michaelbull.result.binding

class StudentNewGradeNotifierImpl(
  private val botEventBus: BotEventBus,
  private val problemStorage: ProblemStorage,
  private val solutionDistributor: SolutionDistributor,
) : StudentNewGradeNotifier {
  override fun notifyStudentOnNewAssignment(
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
