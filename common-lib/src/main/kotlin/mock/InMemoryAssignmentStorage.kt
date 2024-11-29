package com.github.heheteam.commonlib.mock

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.api.AssignmentId
import com.github.heheteam.commonlib.api.AssignmentStorage
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.ProblemStorage

class InMemoryAssignmentStorage : AssignmentStorage {
  val assns = mutableListOf<Assignment>()
  var id = 0L

  override fun resolveAssignment(assignmentId: AssignmentId): Assignment = assns.single { it.id == assignmentId }

  override fun createAssignment(
    courseId: CourseId,
    description: String,
    problemNames: List<String>,
    problemStorage: ProblemStorage,
  ): AssignmentId {
    val problems = problemNames.map { problemStorage.createProblem(AssignmentId(id), it) }
    assns.add(
      Assignment(
        AssignmentId(id),
        description,
        courseId,
      ),
    )
    ++id
    return AssignmentId(id - 1)
  }

  override fun getAssignmentsForCourse(courseId: CourseId): List<AssignmentId> = assns.filter { it.courseId == courseId }.map { it.id }
}
