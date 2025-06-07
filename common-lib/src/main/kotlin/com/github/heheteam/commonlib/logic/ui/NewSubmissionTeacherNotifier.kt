package com.github.heheteam.commonlib.logic.ui

import com.github.heheteam.commonlib.EduPlatformError
import com.github.heheteam.commonlib.Submission
import com.github.heheteam.commonlib.TelegramMessageInfo
import com.github.heheteam.commonlib.interfaces.AssignmentStorage
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.CourseStorage
import com.github.heheteam.commonlib.interfaces.GradeTable
import com.github.heheteam.commonlib.interfaces.ProblemStorage
import com.github.heheteam.commonlib.interfaces.StudentStorage
import com.github.heheteam.commonlib.interfaces.SubmissionDistributor
import com.github.heheteam.commonlib.interfaces.SubmissionId
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.heheteam.commonlib.interfaces.TeacherStorage
import com.github.heheteam.commonlib.interfaces.TelegramTechnicalMessagesStorage
import com.github.heheteam.commonlib.telegram.SubmissionStatusMessageInfo
import com.github.heheteam.commonlib.telegram.TeacherBotTelegramController
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.toResultOr
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.warning
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

@Suppress("LongParameterList") // fix after test exist
internal class NewSubmissionTeacherNotifier(
  private val telegramTechnicalMessageStorage: TelegramTechnicalMessagesStorage,
  private val teacherBotTelegramController: TeacherBotTelegramController,
  private val submissionDistributor: SubmissionDistributor,
  private val problemStorage: ProblemStorage,
  private val assignmentStorage: AssignmentStorage,
  private val studentStorage: StudentStorage,
  private val gradeTable: GradeTable,
  private val teacherStorage: TeacherStorage,
  private val courseStorage: CourseStorage,
) {
  suspend fun notifyNewSubmission(submission: Submission): NewSubmissionNotificationStatus =
    coroutineScope {
      val personalNotificationResult = async {
        val sendingMessageResult = sendSubmissionToTeacherPersonally(submission).getError()
        val teacherId = submission.responsibleTeacherId
        val updatingMenuMessageResult =
          if (teacherId != null) {
            updateMenuMessageInPersonalMessages(teacherId, submission).getError()
          } else {
            null
          }
        TeacherNewSubmissionNotificationStatus(sendingMessageResult, updatingMenuMessageResult)
      }
      val groupNotificationResult = async {
        val sendingMessageStatus = sendSubmissionToGroup(submission).getError()
        val courseId = submissionDistributor.resolveSubmissionCourse(submission.id).get()
        val updatingMenuMessageResult =
          if (courseId != null) {
            updateMenuMessageInGroup(courseId, submission).getError()
          } else null
        GroupNewSubmissionNotificationStatus(sendingMessageStatus, updatingMenuMessageResult)
      }
      return@coroutineScope NewSubmissionNotificationStatus(
        personalNotificationResult.await(),
        groupNotificationResult.await(),
      )
    }

  private suspend fun updateMenuMessageInGroup(
    courseId: CourseId,
    submission: Submission,
  ): Result<Unit, EduPlatformError> {
    return coroutineBinding {
      val menuMessages =
        telegramTechnicalMessageStorage
          .resolveGroupMenuMessage(courseId)
          .mapError { FailedToResolveSubmission(submission, it) }
          .bind()
      deleteMenuMessages(menuMessages)

      val (chatId, messageId) =
        telegramTechnicalMessageStorage
          .resolveGroupFirstUncheckedSubmissionMessage(courseId)
          .mapError { FailedToResolveSubmission(submission, it) }
          .bind()
      teacherBotTelegramController.sendMenuMessage(
        chatId,
        messageId?.let { TelegramMessageInfo(chatId, it) },
      )
    }
  }

  private suspend fun updateMenuMessageInPersonalMessages(
    teacherId: TeacherId,
    submission: Submission,
  ): Result<Unit, EduPlatformError> {
    return coroutineBinding {
      val menuMessages =
        telegramTechnicalMessageStorage
          .resolveTeacherMenuMessage(teacherId)
          .mapError { FailedToResolveSubmission(submission) }
          .bind()
      deleteMenuMessages(menuMessages)

      val (chatId, messageId) =
        telegramTechnicalMessageStorage
          .resolveTeacherFirstUncheckedSubmissionMessage(teacherId)
          .mapError { FailedToResolveSubmission(submission) }
          .bind()
      val menuMessage =
        teacherBotTelegramController
          .sendMenuMessage(chatId, messageId?.let { TelegramMessageInfo(chatId, it) })
          .mapError { FailedToResolveSubmission(submission) }
          .bind()

      telegramTechnicalMessageStorage
        .updateTeacherMenuMessage(TelegramMessageInfo(menuMessage.chatId, menuMessage.messageId))
        .bind()
    }
  }

  private suspend fun sendSubmissionToGroup(
    submission: Submission
  ): Result<Unit, SubmissionSendingError> = coroutineBinding {
    val problem =
      problemStorage
        .resolveProblem(submission.problemId)
        .mapError { FailedToResolveSubmission(submission) }
        .bind()
    val assignment =
      assignmentStorage
        .resolveAssignment(problem.assignmentId)
        .mapError { FailedToResolveSubmission(submission) }
        .bind()

    val chat =
      courseStorage
        .resolveCourseGroup(assignment.courseId)
        .mapError { FailedToResolveSubmission(submission) }
        .bind()
    if (chat == null) {
      Err(SendToGroupSubmissionError(assignment.courseId)).bind<Nothing>()
    }
    teacherBotTelegramController
      .sendSubmission(chat, submission.content)
      .mapError { originalError ->
        SendToGroupSubmissionError(assignment.courseId, causedBy = originalError)
      }
      .bind()
    val submissionStatusInfo =
      extractSubmissionStatusMessageInfo(submission.id)
        .mapError { FailedToResolveSubmission(submission) }
        .bind()
    val groupMessage =
      teacherBotTelegramController
        .sendInitSubmissionStatusMessageInCourseGroupChat(chat, submissionStatusInfo)
        .mapError { originalError ->
          SendToGroupSubmissionError(assignment.courseId, causedBy = originalError)
        }
        .bind()
    telegramTechnicalMessageStorage.registerGroupSubmissionPublication(submission.id, groupMessage)
  }

  private suspend fun sendSubmissionToTeacherPersonally(
    submission: Submission
  ): Result<Unit, SubmissionSendingError> =
    coroutineBinding {
        val teacherId =
          submission.responsibleTeacherId.toResultOr { NoResponsibleTeacherFor(submission) }.bind()
        val teacher =
          teacherStorage
            .resolveTeacher(teacherId)
            .mapError { NoResponsibleTeacherFor(submission) }
            .bind()
        teacherBotTelegramController
          .sendSubmission(teacher.tgId, submission.content)
          .mapError { originalError ->
            SendToTeacherSubmissionError(teacherId, causedBy = originalError)
          }
          .bind()
        val submissionStatusInfo =
          extractSubmissionStatusMessageInfo(submission.id)
            .mapError { FailedToResolveSubmission(submission) }
            .bind()
        val personalTechnicalMessage =
          teacherBotTelegramController
            .sendInitSubmissionStatusMessageDM(teacher.tgId, submissionStatusInfo)
            .mapError { SendToTeacherSubmissionError(teacherId) }
            .bind()
        telegramTechnicalMessageStorage.registerPersonalSubmissionPublication(
          submission.id,
          personalTechnicalMessage,
        )
      }
      .also { println("Returned $it") }

  private fun extractSubmissionStatusMessageInfo(
    submissionId: SubmissionId
  ): Result<SubmissionStatusMessageInfo, EduPlatformError> {
    val result = binding {
      val gradingEntries = gradeTable.getGradingsForSubmission(submissionId).bind()
      val submission = submissionDistributor.resolveSubmission(submissionId).bind()
      val problem = problemStorage.resolveProblem(submission.problemId).bind()
      val assignment = assignmentStorage.resolveAssignment(problem.assignmentId).bind()
      val student = studentStorage.resolveStudent(submission.studentId).bind()
      val responsibleTeacher =
        submission.responsibleTeacherId?.let { teacherStorage.resolveTeacher(it).bind() }
      SubmissionStatusMessageInfo(
        submissionId,
        assignment.description,
        problem.number,
        student,
        responsibleTeacher,
        gradingEntries,
      )
    }
    println(result)
    return result
  }

  private suspend fun deleteMenuMessages(menuMessages: List<TelegramMessageInfo>) {
    menuMessages.forEach { menuMessage ->
      teacherBotTelegramController.deleteMessage(menuMessage).onFailure { error ->
        KSLog.warning("Failed to delete menu message: $error")
      }
    }
  }
}
