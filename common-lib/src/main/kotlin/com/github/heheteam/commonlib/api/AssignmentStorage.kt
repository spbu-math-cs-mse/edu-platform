package com.github.heheteam.commonlib.api

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.ProblemDescription
import com.github.heheteam.commonlib.ResolveError
import com.github.michaelbull.result.Result

interface AssignmentStorage {
  fun resolveAssignment(assignmentId: AssignmentId): Result<Assignment, ResolveError<AssignmentId>>

  fun createAssignment(
    courseId: CourseId,
    description: String,
    problemsDescriptions: List<ProblemDescription>,
    problemStorage: ProblemStorage,
  ): AssignmentId

  fun getAssignmentsForCourse(courseId: CourseId): List<Assignment>
}
