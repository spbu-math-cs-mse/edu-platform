package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.SolutionInputRequest
import com.github.heheteam.commonlib.interfaces.AssignmentStorage
import com.github.heheteam.commonlib.interfaces.CoursesDistributor
import com.github.heheteam.commonlib.interfaces.ProblemStorage
import com.github.heheteam.commonlib.interfaces.ResponsibleTeacherResolver
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.toResultOr

internal class FirstTeacherResolver(
  val problemStorage: ProblemStorage,
  val assignmentStorage: AssignmentStorage,
  val coursesDistributor: CoursesDistributor,
) : ResponsibleTeacherResolver {
  override fun resolveResponsibleTeacher(
    solutionInputRequest: SolutionInputRequest
  ): Result<TeacherId, String> {
    val result =
      binding {
          val problem = problemStorage.resolveProblem(solutionInputRequest.problemId).bind()
          val assignment = assignmentStorage.resolveAssignment(problem.assignmentId).bind()
          val teachers = coursesDistributor.getTeachers(assignment.courseId).sortedBy { it.id.id }
          teachers.firstOrNull()?.id.toResultOr { "No teachers" }.bind()
        }
        .mapError { it.toString() }
    return result
  }
}
