package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.SolutionInputRequest
import com.github.heheteam.commonlib.api.AssignmentStorage
import com.github.heheteam.commonlib.api.CoursesDistributor
import com.github.heheteam.commonlib.api.ProblemStorage
import com.github.heheteam.commonlib.api.ResponsibleTeacherResolver
import com.github.heheteam.commonlib.api.SolutionDistributor
import com.github.heheteam.commonlib.api.TeacherId
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.toResultOr

class RandomTeacherResolver(
  val problemStorage: ProblemStorage,
  val assignmentStorage: AssignmentStorage,
  val coursesDistributor: CoursesDistributor,
  val solutionDistributor: SolutionDistributor,
) : ResponsibleTeacherResolver {
  override fun resolveResponsibleTeacher(solutionInputRequest: SolutionInputRequest): Result<TeacherId, String> {
    val result =
      binding {
        // If teacher has already been assigned
        val teacherId = solutionDistributor.resolveResponsibleTeacher(solutionInputRequest)
        if (teacherId != null) return@binding teacherId

        // Resolve random
        val problem = problemStorage.resolveProblem(solutionInputRequest.problemId).bind()
        val assignment = assignmentStorage.resolveAssignment(problem.assignmentId).bind()
        val teachers = coursesDistributor.getTeachers(assignment.courseId).shuffled()
        println(teachers)
        teachers.firstOrNull()?.id.toResultOr { "No teachers" }.bind()
      }
        .mapError { it.toString() }
    return result
  }
}
