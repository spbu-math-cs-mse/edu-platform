package com.github.heheteam.commonlib.decorators

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.DatabaseExceptionError
import com.github.heheteam.commonlib.EduPlatformError
import com.github.heheteam.commonlib.ProblemDescription
import com.github.heheteam.commonlib.ResolveError
import com.github.heheteam.commonlib.interfaces.AssignmentId
import com.github.heheteam.commonlib.interfaces.AssignmentStorage
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.RatingRecorder
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map

class AssignmentStorageDecorator
internal constructor(
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
  ): Result<AssignmentId, DatabaseExceptionError> =
    assignmentStorage.createAssignment(courseId, description, problemsDescriptions).map {
      ratingRecorder.updateRating(courseId)
      it
    }

  override fun getAssignmentsForCourse(
    courseId: CourseId
  ): Result<List<Assignment>, EduPlatformError> =
    assignmentStorage.getAssignmentsForCourse(courseId)
}
