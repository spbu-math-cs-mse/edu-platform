package com.github.heheteam.commonlib.mock

import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.api.AssignmentId
import com.github.heheteam.commonlib.api.ProblemId
import com.github.heheteam.commonlib.api.ProblemStorage

class InMemoryProblemStorage : ProblemStorage {
  val problems = mutableListOf<Problem>()
  var id = 0L

  override fun resolveProblem(id: ProblemId): Problem = problems.single { it.id == id }

  override fun createProblem(
    assignmentId: AssignmentId,
    number: String,
  ): ProblemId {
    val problem =
      Problem(
        ProblemId(id),
        number,
        "",
        1,
        assignmentId,
      )
    problems.add(problem)
    ++id
    return problem.id
  }

  override fun getProblemsFromAssignment(id: AssignmentId): List<ProblemId> = problems.filter { it.assignmentId == id }.map { it.id }
}
