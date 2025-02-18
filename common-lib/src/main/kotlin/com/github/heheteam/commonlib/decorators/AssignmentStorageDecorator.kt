package com.github.heheteam.commonlib.decorators

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.ProblemDescription
import com.github.heheteam.commonlib.api.AssignmentId
import com.github.heheteam.commonlib.api.AssignmentStorage
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.ProblemStorage
import com.github.heheteam.commonlib.api.RatingRecorder
import com.github.heheteam.commonlib.api.ResolveError
import com.github.michaelbull.result.Result

class AssignmentStorageDecorator(
  private val assignmentStorage: AssignmentStorage,
  private val ratingRecorder: RatingRecorder,
) : AssignmentStorage {
  override fun resolveAssignment(
    assignmentId: AssignmentId
  ): Result<Assignment, ResolveError<AssignmentId>> =
    assignmentStorage.resolveAssignment(assignmentId)

  override fun createAssignment(
    courseId: CourseId,
    description: String,
    problemsDescriptions: List<ProblemDescription>,
    problemStorage: ProblemStorage,
  ): AssignmentId =
    assignmentStorage
      .createAssignment(courseId, description, problemsDescriptions, problemStorage)
      .also { ratingRecorder.updateRating(courseId) }

  override fun getAssignmentsForCourse(courseId: CourseId): List<Assignment> =
    assignmentStorage.getAssignmentsForCourse(courseId)
}
