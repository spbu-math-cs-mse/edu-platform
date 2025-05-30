package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.SubmissionInputRequest
import com.github.heheteam.commonlib.TeacherResolveError
import com.github.heheteam.commonlib.interfaces.AssignmentStorage
import com.github.heheteam.commonlib.interfaces.CourseStorage
import com.github.heheteam.commonlib.interfaces.ProblemStorage
import com.github.heheteam.commonlib.interfaces.ResponsibleTeacherResolver
import com.github.heheteam.commonlib.interfaces.SubmissionDistributor
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.toResultOr

internal class RandomTeacherResolver(
  val problemStorage: ProblemStorage,
  val assignmentStorage: AssignmentStorage,
  val courseStorage: CourseStorage,
  val submissionDistributor: SubmissionDistributor,
) : ResponsibleTeacherResolver {
  override fun resolveResponsibleTeacher(
    submissionInputRequest: SubmissionInputRequest
  ): Result<TeacherId, TeacherResolveError> =
    binding {
        // If teacher has already been assigned
        val teacherId = submissionDistributor.resolveResponsibleTeacher(submissionInputRequest)
        println(teacherId)
        if (teacherId != null) return@binding teacherId

        // Resolve random
        val problem = problemStorage.resolveProblem(submissionInputRequest.problemId).bind()
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
