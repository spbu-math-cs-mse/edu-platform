package com.github.heheteam.commonlib.logic.ui

import com.github.heheteam.commonlib.api.AssignmentStorage
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.ProblemStorage
import com.github.heheteam.commonlib.api.SolutionDistributor
import com.github.heheteam.commonlib.api.SolutionId
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding

class SolutionCourseResolverImpl(
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
