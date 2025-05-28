package com.github.heheteam.commonlib.logic.ui

import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.TelegramMessageInfo
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.SolutionDistributor
import com.github.heheteam.commonlib.interfaces.SolutionId
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.heheteam.commonlib.interfaces.TelegramTechnicalMessagesStorage
import com.github.heheteam.commonlib.telegram.TeacherBotTelegramController
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.get
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.onFailure
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.error
import dev.inmo.kslog.common.warning
import dev.inmo.tgbotapi.bot.exceptions.CommonRequestException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

internal class UiControllerTelegramSender(
  private val studentNotifier: StudentNewGradeNotifier,
  private val journalUpdater: JournalUpdater,
  private val solutionDistributor: SolutionDistributor,
  private val teacherBotTelegramController: TeacherBotTelegramController,
  private val telegramTechnicalMessageStorage: TelegramTechnicalMessagesStorage,
) : UiController {
  override fun updateUiOnSolutionAssessment(
    solutionId: SolutionId,
    assessment: SolutionAssessment,
  ) {
    runBlocking(Dispatchers.IO) {
      studentNotifier.notifyStudentOnNewAssessment(solutionId, assessment)
    }
    runBlocking(Dispatchers.IO) { journalUpdater.updateJournalDisplaysForSolution(solutionId) }
    val teacherId = solutionDistributor.resolveSolution(solutionId).get()?.responsibleTeacherId
    if (teacherId != null) {
      runBlocking(Dispatchers.IO) { updateMenuMessageInPersonalMessages(teacherId) }
        .onFailure { KSLog.error(it) }
    }

    val courseId = solutionDistributor.resolveSolutionCourse(solutionId).get()
    if (courseId != null) {
      runBlocking(Dispatchers.IO) { updateMenuMessageInGroup(courseId) }
        .onFailure { KSLog.error(it) }
    }
  }

  private suspend fun updateMenuMessageInPersonalMessages(
    teacherId: TeacherId
  ): Result<Unit, String> {
    return coroutineBinding {
      val menuMessages = telegramTechnicalMessageStorage.resolveTeacherMenuMessage(teacherId).bind()
      deleteMenuMessages(menuMessages)

      val (chatId, messageId) =
        telegramTechnicalMessageStorage
          .resolveTeacherFirstUncheckedSolutionMessage(teacherId)
          .bind()
      val menuMessage =
        teacherBotTelegramController
          .sendMenuMessage(chatId, messageId?.let { TelegramMessageInfo(chatId, it) })
          .mapError { it.toString() }
          .bind()

      telegramTechnicalMessageStorage.updateTeacherMenuMessage(
        TelegramMessageInfo(menuMessage.chatId, menuMessage.messageId)
      )
    }
  }

  private suspend fun updateMenuMessageInGroup(courseId: CourseId): Result<Unit, String> =
    coroutineBinding {
      val menuMessages = telegramTechnicalMessageStorage.resolveGroupMenuMessage(courseId).bind()
      deleteMenuMessages(menuMessages)

      val (chatId, messageId) =
        telegramTechnicalMessageStorage.resolveGroupFirstUncheckedSolutionMessage(courseId).bind()
      val menuMessage =
        teacherBotTelegramController
          .sendMenuMessage(chatId, messageId?.let { TelegramMessageInfo(chatId, it) })
          .mapError { it.toString() }
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
