package com.github.heheteam.commonlib.logic

import com.github.heheteam.commonlib.ProblemDescription
import com.github.heheteam.commonlib.errors.CourseService
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.heheteam.commonlib.errors.NamedError
import com.github.heheteam.commonlib.interfaces.AdminStorage
import com.github.heheteam.commonlib.interfaces.AssignmentId
import com.github.heheteam.commonlib.interfaces.AssignmentStorage
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.StudentStorage
import com.github.heheteam.commonlib.telegram.AdminBotTelegramController
import com.github.heheteam.commonlib.telegram.StudentBotTelegramController
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.flatMap
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapResult

class ChallengeService
internal constructor(
  private val adminStorage: AdminStorage,
  private val studentStorage: StudentStorage,
  private val assignmentStorage: AssignmentStorage,
  private val courseService: CourseService,
  private val adminBotTelegramController: AdminBotTelegramController,
  private val studentBotTelegramController: StudentBotTelegramController,
) {
  suspend fun requestChallengeAccess(
    studentId: StudentId,
    courseId: CourseId,
  ): Result<Unit, EduPlatformError> =
    adminStorage.getAdmins().flatMap { admins ->
      admins
        .mapResult { admin ->
          adminBotTelegramController.notifyAdminOnNewChallengeAccessRequest(
            admin.tgId,
            studentId,
            courseId,
          )
        }
        .map {}
    }

  suspend fun grantAccessToChallenge(
    studentId: StudentId,
    courseId: CourseId,
  ): Result<Unit, EduPlatformError> = coroutineBinding {
    assignmentStorage.grantAccessToChallenge(studentId, courseId).bind()
    val course =
      courseService.resolveCourse(courseId).bind()?.toLegacy()
        ?: Err(
            NamedError(
              "Cannot grant access to the challenge because the student was not found: $studentId"
            )
          )
          .bind()

    val student =
      studentStorage.resolveStudent(studentId).bind()
        ?: Err(
            NamedError(
              "Cannot grant access to the challenge because the course was not found: $courseId"
            )
          )
          .bind()

    studentBotTelegramController
      .notifyStudentOnGrantedAccessToChallenge(student.tgId, course)
      .bind()
  }

  fun createChallenge(
    courseId: CourseId,
    assignmentId: AssignmentId,
    challengeDescription: String,
    challengingProblemsDescriptions: List<ProblemDescription>,
    statementsUrl: String?,
  ): Result<AssignmentId?, EduPlatformError> = binding {
    assignmentStorage
      .createChallenge(
        assignmentId,
        courseId,
        challengeDescription,
        statementsUrl,
        challengingProblemsDescriptions,
      )
      .bind()
  }
}
