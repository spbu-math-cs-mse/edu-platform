package com.github.heheteam.commonlib.api

import com.github.heheteam.commonlib.Assignment

interface AssignmentStorage {
  fun resolveAssignment(assignmentId: AssignmentId): Assignment

  fun createAssignment(
    courseId: CourseId,
    description: String,
    problemNames: List<String>,
    problemStorage: ProblemStorage,
  ): AssignmentId

  fun getAssignmentsForCourse(courseId: CourseId): List<AssignmentId>
}