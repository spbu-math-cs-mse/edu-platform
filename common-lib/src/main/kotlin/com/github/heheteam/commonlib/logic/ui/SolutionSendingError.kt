package com.github.heheteam.commonlib.logic.ui

import com.github.heheteam.commonlib.EduPlatformError
import com.github.heheteam.commonlib.Solution
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.TeacherId

abstract class SolutionSendingError(override val causedBy: EduPlatformError?) : EduPlatformError

class NoResponsibleTeacherFor(
  val solution: Solution,
  override val causedBy: EduPlatformError? = null,
) : SolutionSendingError(causedBy) {
  override val shortDescription: String
    get() = "No teacher is responsible for solution $solution"
}

class FailedToResolveSolution(
  val solution: Solution,
  override val causedBy: EduPlatformError? = null,
) : SolutionSendingError(causedBy) {
  override val shortDescription: String
    get() = "Failed ot find solution id=${solution.id} in the database"
}

class SendToGroupSolutionError(
  val courseId: CourseId,
  override val causedBy: EduPlatformError? = null,
) : SolutionSendingError(causedBy) {
  override val shortDescription: String
    get() = "Failed to send a solution to the group"
}

class SendToTeacherSolutionError(
  val teacherId: TeacherId,
  override val causedBy: EduPlatformError? = null,
) : SolutionSendingError(causedBy) {
  override val shortDescription: String
    get() = "Failed to send submission to a group"
}
