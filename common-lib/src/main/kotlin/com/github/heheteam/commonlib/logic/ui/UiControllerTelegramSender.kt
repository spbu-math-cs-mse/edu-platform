package com.github.heheteam.commonlib.logic.ui

import com.github.heheteam.commonlib.SubmissionAssessment
import com.github.heheteam.commonlib.TelegramMessageInfo
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.SubmissionDistributor
import com.github.heheteam.commonlib.interfaces.SubmissionId
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.heheteam.commonlib.interfaces.TelegramTechnicalMessagesStorage
import com.github.heheteam.commonlib.telegram.TeacherBotTelegramController
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.get
import com.github.michaelbull.result.onFailure
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.error
import dev.inmo.kslog.common.warning
import dev.inmo.tgbotapi.bot.exceptions.CommonRequestException

internal class UiControllerTelegramSender(
  private val studentNotifier: StudentNewGradeNotifier,
  private val journalUpdater: JournalUpdater,
  private val submissionDistributor: SubmissionDistributor,
  private val teacherBotTelegramController: TeacherBotTelegramController,
  private val telegramTechnicalMessageStorage: TelegramTechnicalMessagesStorage,
) : UiController {
  override suspend fun updateUiOnSubmissionAssessment(
    submissionId: SubmissionId,
    assessment: SubmissionAssessment,
  ) {
    studentNotifier.notifyStudentOnNewAssessment(submissionId, assessment)
    journalUpdater.updateJournalDisplaysForSubmission(submissionId).onFailure { KSLog.error(it) }
    val teacherId =
      submissionDistributor.resolveSubmission(submissionId).get()?.responsibleTeacherId
    if (teacherId != null) {
      updateMenuMessageInPersonalMessages(teacherId).onFailure { KSLog.error(it) }
    }

    val courseId = submissionDistributor.resolveSubmissionCourse(submissionId).get()
    if (courseId != null) {
      updateMenuMessageInGroup(courseId).onFailure { KSLog.error(it) }
    }
  }

  private suspend fun updateMenuMessageInPersonalMessages(
    teacherId: TeacherId
  ): Result<Unit, EduPlatformError> {
    return coroutineBinding {
      val menuMessages = telegramTechnicalMessageStorage.resolveTeacherMenuMessage(teacherId).bind()
      deleteMenuMessages(menuMessages)

      val (chatId, messageId) =
        telegramTechnicalMessageStorage
          .resolveTeacherFirstUncheckedSubmissionMessage(teacherId)
          .bind()
      val menuMessage =
        teacherBotTelegramController
          .sendPersonalMenuMessage(
            teacherId,
            chatId,
            messageId?.let { TelegramMessageInfo(chatId, it) },
          )
          .bind()

      telegramTechnicalMessageStorage.updateTeacherMenuMessage(
        TelegramMessageInfo(menuMessage.chatId, menuMessage.messageId)
      )
    }
  }

  private suspend fun updateMenuMessageInGroup(courseId: CourseId): Result<Unit, EduPlatformError> =
    coroutineBinding {
      val menuMessages = telegramTechnicalMessageStorage.resolveGroupMenuMessage(courseId).bind()
      deleteMenuMessages(menuMessages)

      val (chatId, messageId) =
        telegramTechnicalMessageStorage.resolveGroupFirstUncheckedSubmissionMessage(courseId).bind()
      val menuMessage =
        teacherBotTelegramController
          .sendGroupMenuMessage(
            courseId,
            chatId,
            messageId?.let { TelegramMessageInfo(chatId, it) },
          )
          .bind()
      telegramTechnicalMessageStorage.updateTeacherMenuMessage(
        TelegramMessageInfo(menuMessage.chatId, menuMessage.messageId)
      )
    }

  private suspend fun deleteMenuMessages(menuMessages: List<TelegramMessageInfo>) {
    menuMessages.map { menuMessage ->
      try {
        teacherBotTelegramController.deleteMessage(menuMessage)
      } catch (e: CommonRequestException) {
        KSLog.warning("Menu message has already been deleted:\n${e.message}")
      }
    }
  }
}
