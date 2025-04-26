package com.github.heheteam.commonlib.logic.ui

import com.github.heheteam.commonlib.TelegramMessageInfo
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.heheteam.commonlib.interfaces.TelegramTechnicalMessagesStorage
import com.github.heheteam.commonlib.telegram.TeacherBotTelegramController
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.mapError
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.warning
import dev.inmo.tgbotapi.bot.exceptions.CommonRequestException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class MenuMessageUpdaterImpl
internal constructor(
  private val technicalMessageStorage: TelegramTechnicalMessagesStorage,
  private val teacherBotTelegramController: TeacherBotTelegramController,
) : MenuMessageUpdater {
  override fun updateMenuMessageInPersonalChat(teacherId: TeacherId): Result<Unit, String> =
    runBlocking(Dispatchers.IO) {
      return@runBlocking coroutineBinding {
        val menuMessages = technicalMessageStorage.resolveTeacherMenuMessage(teacherId).bind()
        deleteMenuMessages(menuMessages)
        val (chatId, messageId) =
          technicalMessageStorage.resolveTeacherFirstUncheckedSolutionMessage(teacherId).bind()
        val menuMessage =
          if (messageId != null) {
              teacherBotTelegramController.sendMenuMessage(
                chatId,
                TelegramMessageInfo(chatId, messageId),
              )
            } else {
              teacherBotTelegramController.sendMenuMessage(chatId, null)
            }
            .mapError { it.toString() }
            .bind()
        technicalMessageStorage.updateTeacherMenuMessage(
          TelegramMessageInfo(menuMessage.chatId, menuMessage.messageId)
        )
      }
    }

  override fun updateMenuMessageInGroupChat(courseId: CourseId): Result<Unit, String> =
    runBlocking(Dispatchers.IO) {
      coroutineBinding {
        val menuMessages = technicalMessageStorage.resolveGroupMenuMessage(courseId).bind()
        deleteMenuMessages(menuMessages)
        val (chatId, messageId) =
          technicalMessageStorage.resolveGroupFirstUncheckedSolutionMessage(courseId).bind()
        val menuMessage =
          if (messageId != null) {
              teacherBotTelegramController.sendMenuMessage(
                chatId,
                TelegramMessageInfo(chatId, messageId),
              )
            } else {
              teacherBotTelegramController.sendMenuMessage(chatId, null)
            }
            .mapError { it.toString() }
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
