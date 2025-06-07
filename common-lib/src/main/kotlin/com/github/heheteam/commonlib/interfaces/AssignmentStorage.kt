package com.github.heheteam.commonlib.interfaces

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.DatabaseExceptionError
import com.github.heheteam.commonlib.EduPlatformError
import com.github.heheteam.commonlib.ProblemDescription
import com.github.heheteam.commonlib.ResolveError
import com.github.michaelbull.result.Result

internal interface AssignmentStorage {
  fun resolveAssignment(assignmentId: AssignmentId): Result<Assignment, ResolveError<AssignmentId>>

  fun createAssignment(
    courseId: CourseId,
    description: String,
    problemsDescriptions: List<ProblemDescription>,
  ): Result<AssignmentId, DatabaseExceptionError>

  fun getAssignmentsForCourse(courseId: CourseId): Result<List<Assignment>, EduPlatformError>
}
