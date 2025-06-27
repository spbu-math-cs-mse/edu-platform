package com.github.heheteam.commonlib.errors

import com.github.heheteam.commonlib.Submission
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.TeacherId

abstract class SubmissionSendingError(override val causedBy: EduPlatformError?) : EduPlatformError

class NoResponsibleTeacherFor(
  val submission: Submission,
  override val causedBy: EduPlatformError? = null,
) : SubmissionSendingError(causedBy) {
  override val shortDescription: String
    get() = "No teacher is responsible for submission $submission"
}

class FailedToResolveSubmission(
  val submission: Submission,
  override val causedBy: EduPlatformError? = null,
) : SubmissionSendingError(causedBy) {
  override val shortDescription: String
    get() = "Failed ot find submission id=${submission.id} in the database"
}

class SendToGroupSubmissionError(
  val courseId: CourseId,
  override val causedBy: EduPlatformError? = null,
) : SubmissionSendingError(causedBy) {
  override val shortDescription: String = "Failed to send a submission to the group"
}

class SendToTeacherSubmissionError(
  val teacherId: TeacherId,
  override val causedBy: EduPlatformError? = null,
) : SubmissionSendingError(causedBy) {
  override val shortDescription: String = "Failed to send submission to a group"
}
