package com.github.heheteam.commonlib.api

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.Grade
import com.github.michaelbull.result.Result

interface AssignmentStorage {
  fun resolveAssignment(assignmentId: AssignmentId): Result<Assignment, ResolveError<AssignmentId>>

  fun createAssignment(
    courseId: CourseId,
    description: String,
    problemsDescriptions: List<Triple<String, String, Grade>>,
    problemStorage: ProblemStorage,
  ): AssignmentId

  fun getAssignmentsForCourse(courseId: CourseId): List<Assignment>
}
