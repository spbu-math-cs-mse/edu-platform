package com.github.heheteam.commonlib.logic.ui

import com.github.heheteam.commonlib.interfaces.AssignmentStorage
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.ProblemStorage
import com.github.heheteam.commonlib.interfaces.SolutionDistributor
import com.github.heheteam.commonlib.interfaces.SolutionId
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding

class SolutionCourseResolverImpl
internal constructor(
  private val solutionStorage: SolutionDistributor,
  private val problemStorage: ProblemStorage,
  private val assignmentStorage: AssignmentStorage,
) : SolutionCourseResolver {
  override fun resolveCourse(solutionId: SolutionId): Result<CourseId, Any> = binding {
    val solution = solutionStorage.resolveSolution(solutionId).bind()
    val problem = problemStorage.resolveProblem(solution.problemId).bind()
    val assignment = assignmentStorage.resolveAssignment(problem.assignmentId).bind()
    assignment.courseId
  }
}
