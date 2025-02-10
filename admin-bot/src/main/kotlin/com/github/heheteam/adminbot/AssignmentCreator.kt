package com.github.heheteam.adminbot

import com.github.heheteam.commonlib.ProblemDescription
import com.github.heheteam.commonlib.api.AssignmentStorage
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.ProblemStorage

class AssignmentCreator(
  private val assignmentStorage: AssignmentStorage,
  private val problemStorage: ProblemStorage,
) {
  fun createAssignment(
    courseId: CourseId,
    description: String,
    problemsDescriptions: List<ProblemDescription>,
  ) {
    assignmentStorage.createAssignment(courseId, description, problemsDescriptions, problemStorage)
  }
}
