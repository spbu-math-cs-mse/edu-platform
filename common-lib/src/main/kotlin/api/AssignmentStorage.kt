package com.github.heheteam.commonlib.api

import com.github.heheteam.commonlib.Assignment

interface AssignmentStorage {
  fun resolveAssignment(id: AssignmentId): Assignment
  fun createAssignment(
    courseId: CourseId,
    description: String,
    problemNames: List<String>,
    problemStorage: ProblemStorage,
  ): AssignmentId
}
