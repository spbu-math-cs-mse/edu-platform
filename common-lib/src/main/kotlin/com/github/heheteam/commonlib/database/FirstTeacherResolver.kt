package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.SubmissionInputRequest
import com.github.heheteam.commonlib.TeacherResolveError
import com.github.heheteam.commonlib.interfaces.AssignmentStorage
import com.github.heheteam.commonlib.interfaces.CourseStorage
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
  val courseStorage: CourseStorage,
) : ResponsibleTeacherResolver {
  override fun resolveResponsibleTeacher(
    submissionInputRequest: SubmissionInputRequest
  ): Result<TeacherId, TeacherResolveError> {
    val result =
      binding {
          val problem = problemStorage.resolveProblem(submissionInputRequest.problemId).bind()
          val assignment = assignmentStorage.resolveAssignment(problem.assignmentId).bind()
          val teachers =
            courseStorage.getTeachers(assignment.courseId).bind().sortedBy { it.id.long }
          teachers
            .firstOrNull()
            ?.id
            .toResultOr {
              TeacherResolveError(message = "No teachers in course id=${assignment.courseId}")
            }
            .bind()
        }
        .mapError { error -> TeacherResolveError(message = error.toString(), causedBy = error) }
    return result
  }
}
