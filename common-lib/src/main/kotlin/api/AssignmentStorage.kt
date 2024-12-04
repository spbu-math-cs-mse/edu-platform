package com.github.heheteam.commonlib.api

import com.github.heheteam.commonlib.Assignment
import com.github.michaelbull.result.Result

interface AssignmentStorage {
  fun resolveAssignment(assignmentId: AssignmentId): Result<Assignment, ResolveError>

  fun createAssignment(
    courseId: CourseId,
    description: String,
    problemNames: List<String>,
    problemStorage: ProblemStorage,
  ): AssignmentId

  fun getAssignmentsForCourse(courseId: CourseId): List<Assignment>
}
