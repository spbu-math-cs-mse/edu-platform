package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.SolutionInputRequest
import com.github.heheteam.commonlib.TeacherResolveError
import com.github.heheteam.commonlib.interfaces.AssignmentStorage
import com.github.heheteam.commonlib.interfaces.CourseStorage
import com.github.heheteam.commonlib.interfaces.ProblemStorage
import com.github.heheteam.commonlib.interfaces.ResponsibleTeacherResolver
import com.github.heheteam.commonlib.interfaces.SolutionDistributor
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.toResultOr

internal class RandomTeacherResolver(
  val problemStorage: ProblemStorage,
  val assignmentStorage: AssignmentStorage,
  val courseStorage: CourseStorage,
  val solutionDistributor: SolutionDistributor,
) : ResponsibleTeacherResolver {
  override fun resolveResponsibleTeacher(
    solutionInputRequest: SolutionInputRequest
  ): Result<TeacherId, TeacherResolveError> =
    binding {
        // If teacher has already been assigned
        val teacherId = solutionDistributor.resolveResponsibleTeacher(solutionInputRequest)
        println(teacherId)
        if (teacherId != null) return@binding teacherId

        // Resolve random
        val problem = problemStorage.resolveProblem(solutionInputRequest.problemId).bind()
        val assignment = assignmentStorage.resolveAssignment(problem.assignmentId).bind()
        val teachers = courseStorage.getTeachers(assignment.courseId).shuffled()
        println(teachers)
        teachers
          .firstOrNull()
          ?.id
          .toResultOr { TeacherResolveError(message = "No teachers") }
          .bind()
      }
      .mapError { error -> TeacherResolveError(message = error.toString(), causedBy = error) }
}
