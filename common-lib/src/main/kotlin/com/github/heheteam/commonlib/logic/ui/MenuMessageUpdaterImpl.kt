package com.github.heheteam.commonlib.logic.ui

import com.github.heheteam.commonlib.TelegramMessageInfo
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.heheteam.commonlib.interfaces.TelegramTechnicalMessagesStorage
import com.github.heheteam.commonlib.telegram.TeacherBotTelegramController
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.warning
import dev.inmo.tgbotapi.bot.exceptions.CommonRequestException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MenuMessageUpdaterImpl
internal constructor(
  private val technicalMessageStorage: TelegramTechnicalMessagesStorage,
  private val teacherBotTelegramController: TeacherBotTelegramController,
) : MenuMessageUpdater {
  override suspend fun updateMenuMessageInPersonalChat(
    teacherId: TeacherId
  ): Result<Unit, EduPlatformError> = coroutineBinding {
    val menuMessages = technicalMessageStorage.resolveTeacherMenuMessage(teacherId).bind()
    deleteMenuMessages(menuMessages)
    val (chatId, messageId) =
      technicalMessageStorage.resolveTeacherFirstUncheckedSubmissionMessage(teacherId).bind()
    val menuMessage =
      if (messageId != null) {
          teacherBotTelegramController.sendMenuMessage(
            chatId,
            TelegramMessageInfo(chatId, messageId),
          )
        } else {
          teacherBotTelegramController.sendMenuMessage(chatId, null)
        }
        .bind()
    technicalMessageStorage.updateTeacherMenuMessage(
      TelegramMessageInfo(menuMessage.chatId, menuMessage.messageId)
    )
  }

  override suspend fun updateMenuMessageInGroupChat(
    courseId: CourseId
  ): Result<Unit, EduPlatformError> =
    withContext(Dispatchers.IO) {
      coroutineBinding {
        val menuMessages = technicalMessageStorage.resolveGroupMenuMessage(courseId).bind()
        deleteMenuMessages(menuMessages)
        val (chatId, messageId) =
          technicalMessageStorage.resolveGroupFirstUncheckedSubmissionMessage(courseId).bind()
        val menuMessage =
          if (messageId != null) {
              teacherBotTelegramController.sendMenuMessage(
                chatId,
                TelegramMessageInfo(chatId, messageId),
              )
            } else {
              teacherBotTelegramController.sendMenuMessage(chatId, null)
            }
            .bind()
        technicalMessageStorage.updateTeacherMenuMessage(
          TelegramMessageInfo(menuMessage.chatId, menuMessage.messageId)
        )
      }
    }

  private suspend fun deleteMenuMessages(menuMessages: List<TelegramMessageInfo>) {
    menuMessages.map { menuMessage ->
      try {
        teacherBotTelegramController.deleteMessage(
          TelegramMessageInfo(menuMessage.chatId, menuMessage.messageId)
        )
      } catch (e: CommonRequestException) {
        KSLog.warning("Menu message has already been deleted:\n$e")
      }
    }
  }
}
