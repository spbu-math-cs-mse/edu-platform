package com.github.heheteam.teacherbot.logic

import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.api.BotEventBus
import com.github.heheteam.commonlib.api.ProblemStorage
import com.github.heheteam.commonlib.api.SolutionDistributor
import com.github.heheteam.commonlib.api.SolutionId
import com.github.michaelbull.result.binding
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class StudentNewGradeNotifierImpl : StudentNewGradeNotifier, KoinComponent {
  private val botEventBus: BotEventBus by inject()
  private val problemStorage: ProblemStorage by inject()
  private val solutionDistributor: SolutionDistributor by inject()

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
