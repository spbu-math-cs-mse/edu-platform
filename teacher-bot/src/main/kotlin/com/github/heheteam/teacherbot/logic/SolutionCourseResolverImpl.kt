package com.github.heheteam.teacherbot.logic

import com.github.heheteam.commonlib.api.AssignmentStorage
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.ProblemStorage
import com.github.heheteam.commonlib.api.SolutionDistributor
import com.github.heheteam.commonlib.api.SolutionId
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SolutionCourseResolverImpl : SolutionCourseResolver, KoinComponent {
  private val solutionStorage: SolutionDistributor by inject()
  private val problemStorage: ProblemStorage by inject()
  private val assignmentStorage: AssignmentStorage by inject()

  override fun resolveCourse(solutionId: SolutionId): Result<CourseId, Any> = binding {
    val solution = solutionStorage.resolveSolution(solutionId).bind()
    val problem = problemStorage.resolveProblem(solution.problemId).bind()
    val assignment = assignmentStorage.resolveAssignment(problem.assignmentId).bind()
    assignment.courseId
  }
}
