package com.github.heheteam.commonlib.logic.ui

import com.github.heheteam.commonlib.EduPlatformError

data class TeacherNewSubmissionNotificationStatus(
  val teacherDirectMessagingSendError: SubmissionSendingError? = null,
  val teacherMenuUpdateError: EduPlatformError? = null,
) {
  fun isSuccess() = teacherMenuUpdateError == null && teacherDirectMessagingSendError == null
}

data class GroupNewSubmissionNotificationStatus(
  val groupDirectMessageSendingError: SubmissionSendingError? = null,
  val groupMenuUpdateError: EduPlatformError? = null,
) {
  fun isSuccess() = groupDirectMessageSendingError == null && groupMenuUpdateError == null
}

data class NewSubmissionNotificationStatus(
  val teacherNewSubmissionNotificationStatus: TeacherNewSubmissionNotificationStatus,
  val groupNewSubmissionNotificationStatus: GroupNewSubmissionNotificationStatus,
) {
  fun isSuccess() =
    teacherNewSubmissionNotificationStatus.isSuccess() &&
      groupNewSubmissionNotificationStatus.isSuccess()
}

data class NotificationError(
  val newSubmissionNotificationStatus: NewSubmissionNotificationStatus,
  override val causedBy: EduPlatformError? = null,
) : EduPlatformError {
  override val shortDescription: String =
    "Error while notifying on new solution; status=$newSubmissionNotificationStatus"
}

fun defaultNewSolutionNotificationStatus(): NewSubmissionNotificationStatus =
  NewSubmissionNotificationStatus(
    TeacherNewSubmissionNotificationStatus(),
    GroupNewSubmissionNotificationStatus(),
  )
