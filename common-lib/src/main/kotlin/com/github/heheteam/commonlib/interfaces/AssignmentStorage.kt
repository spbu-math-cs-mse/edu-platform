package com.github.heheteam.commonlib.interfaces

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.ProblemDescription
import com.github.heheteam.commonlib.errors.DatabaseExceptionError
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.heheteam.commonlib.errors.ResolveError
import com.github.michaelbull.result.Result

interface AssignmentStorage {
  fun resolveAssignment(assignmentId: AssignmentId): Result<Assignment, ResolveError<AssignmentId>>

  fun createAssignment(
    courseId: CourseId,
    description: String,
    statementsUrl: String?,
    problemsDescriptions: List<ProblemDescription>,
  ): Result<AssignmentId, DatabaseExceptionError>

  fun createChallenge(
    assignmentId: AssignmentId,
    courseId: CourseId,
    description: String,
    statementsUrl: String?,
    problemsDescriptions: List<ProblemDescription>,
  ): Result<AssignmentId, DatabaseExceptionError>

  fun grantAccessToChallenge(
    studentId: StudentId,
    courseId: CourseId,
  ): Result<Unit, DatabaseExceptionError>

  fun getAssignmentsForCourse(courseId: CourseId): Result<List<Assignment>, EduPlatformError>
}
