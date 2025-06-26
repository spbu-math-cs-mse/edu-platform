package com.github.heheteam.commonlib.logic.ui

import com.github.heheteam.commonlib.SubmissionAssessment
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.heheteam.commonlib.errors.SubmissionSendingError
import com.github.heheteam.commonlib.interfaces.SubmissionId
import com.github.michaelbull.result.Result

interface StudentNewGradeNotifier {
  suspend fun notifyStudentOnNewAssessment(
    submissionId: SubmissionId,
    assessment: SubmissionAssessment,
  ): Result<Unit, EduPlatformError>
}

data class TeacherNewSubmissionNotificationStatus(
  val teacherDirectMessagingSendError: EduPlatformError? = null,
  val teacherMenuUpdateError: EduPlatformError? = null,
)

data class GroupNewSubmissionNotificationStatus(
  val groupDirectMessageSendingError: SubmissionSendingError? = null,
  val groupMenuUpdateError: EduPlatformError? = null,
)

data class NewSubmissionNotificationStatus(
  val teacherNewSubmissionNotificationStatus: TeacherNewSubmissionNotificationStatus,
  val groupNewSubmissionNotificationStatus: GroupNewSubmissionNotificationStatus,
)
