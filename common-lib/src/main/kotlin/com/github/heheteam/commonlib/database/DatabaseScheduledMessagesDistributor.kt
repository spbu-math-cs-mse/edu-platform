package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.DatabaseExceptionError
import com.github.heheteam.commonlib.EduPlatformError
import com.github.heheteam.commonlib.NewScheduledMessageInfo
import com.github.heheteam.commonlib.ResolveError
import com.github.heheteam.commonlib.database.table.ScheduledMessageTable
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.ScheduledMessage
import com.github.heheteam.commonlib.interfaces.ScheduledMessageId
import com.github.heheteam.commonlib.interfaces.ScheduledMessagesDistributor
import com.github.heheteam.commonlib.interfaces.SentMessageLogStorage
import com.github.heheteam.commonlib.interfaces.toScheduledMessageId
import com.github.heheteam.commonlib.telegram.StudentBotTelegramController
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.runCatching
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class DatabaseScheduledMessagesDistributor(
  private val database: Database,
  private val sentMessageLogStorage: SentMessageLogStorage,
  private val studentBotTelegramController: StudentBotTelegramController,
) : ScheduledMessagesDistributor {
  override fun sendScheduledMessage(
    adminId: AdminId,
    messageInfo: NewScheduledMessageInfo,
  ): Result<ScheduledMessageId, EduPlatformError> =
    transaction(database) {
      runCatching {
          val id =
            ScheduledMessageTable.insertAndGetId {
              it[timestamp] = messageInfo.timestamp.toKotlinLocalDateTime()
              it[content] = messageInfo.content
              it[shortName] = messageInfo.shortName
              it[courseId] = messageInfo.courseId.long
              it[isSent] = false
              it[isDeleted] = false
              it[this.adminId] = adminId.long
            }
          id.value.toScheduledMessageId()
        }
        .mapError { DatabaseExceptionError(it) }
    }

  override fun resolveScheduledMessage(
    scheduledMessageId: ScheduledMessageId
  ): Result<ScheduledMessage, EduPlatformError> = binding {
    val row =
      runCatching {
          transaction(database) {
            ScheduledMessageTable.selectAll()
              .where { ScheduledMessageTable.id eq scheduledMessageId.long }
              .singleOrNull()
          }
        }
        .mapError { DatabaseExceptionError(it) }
        .bind()

    row?.let {
      ScheduledMessage(
        id = it[ScheduledMessageTable.id].value.toScheduledMessageId(),
        timestamp = it[ScheduledMessageTable.timestamp],
        content = it[ScheduledMessageTable.content],
        shortName = it[ScheduledMessageTable.shortName],
        courseId = CourseId(it[ScheduledMessageTable.courseId]),
        isSent = it[ScheduledMessageTable.isSent],
        isDeleted = it[ScheduledMessageTable.isDeleted],
        adminId = AdminId(it[ScheduledMessageTable.adminId]),
      )
    } ?: Err(ResolveError(scheduledMessageId, "ScheduledMessage")).bind()
  }

  override fun viewScheduledMessages(
    adminId: AdminId?,
    courseId: CourseId?,
    lastN: Int,
  ): List<ScheduledMessage> =
    transaction(database) {
      val query = ScheduledMessageTable.selectAll()
      adminId?.let { query.andWhere { ScheduledMessageTable.adminId eq it.long } }
      courseId?.let { query.andWhere { ScheduledMessageTable.courseId eq it.long } }

      query
        .orderBy(ScheduledMessageTable.timestamp to org.jetbrains.exposed.sql.SortOrder.DESC)
        .limit(lastN)
        .map {
          ScheduledMessage(
            id = it[ScheduledMessageTable.id].value.toScheduledMessageId(),
            timestamp = it[ScheduledMessageTable.timestamp],
            content = it[ScheduledMessageTable.content],
            shortName = it[ScheduledMessageTable.shortName],
            courseId = CourseId(it[ScheduledMessageTable.courseId]),
            isSent = it[ScheduledMessageTable.isSent],
            isDeleted = it[ScheduledMessageTable.isDeleted],
            adminId = AdminId(it[ScheduledMessageTable.adminId]),
          )
        }
    }

  override suspend fun deleteScheduledMessage(
    scheduledMessageId: ScheduledMessageId
  ): Result<Unit, EduPlatformError> = binding {
    val updatedRows =
      runCatching {
          transaction(database) {
            ScheduledMessageTable.update({ ScheduledMessageTable.id eq scheduledMessageId.long }) {
              it[isDeleted] = true
            }
          }
        }
        .mapError { DatabaseExceptionError(it) }
        .bind()

    if (updatedRows == 0) {
      Err(ResolveError(scheduledMessageId, "ScheduledMessage")).bind()
    } else {
      val sentLogs = sentMessageLogStorage.getSentMessageLogs(scheduledMessageId)
      sentLogs.forEach { log ->
        runBlocking {
            studentBotTelegramController.deleteMessage(log.chatId, log.telegramMessageId)
          }
          .onFailure { error ->
            println(
              "Failed to delete Telegram message for scheduled message " +
                "${scheduledMessageId.long}, log ${log.logId}: ${error.shortDescription}"
            )
          }
      }
    }
  }

  override fun getMessagesUpToDate(date: LocalDateTime): List<ScheduledMessage> {
    return transaction(database) {
      ScheduledMessageTable.selectAll()
        .where {
          (ScheduledMessageTable.timestamp lessEq date) and
            (ScheduledMessageTable.isSent eq false) and
            (ScheduledMessageTable.isDeleted eq false)
        }
        .map {
          ScheduledMessage(
            id = it[ScheduledMessageTable.id].value.toScheduledMessageId(),
            timestamp = it[ScheduledMessageTable.timestamp],
            content = it[ScheduledMessageTable.content],
            shortName = it[ScheduledMessageTable.shortName],
            courseId = CourseId(it[ScheduledMessageTable.courseId]),
            isSent = it[ScheduledMessageTable.isSent],
            isDeleted = it[ScheduledMessageTable.isDeleted],
            adminId = AdminId(it[ScheduledMessageTable.adminId]),
          )
        }
    }
  }

  override fun markMessagesUpToDateAsSent(date: LocalDateTime): Unit =
    transaction(database) {
      ScheduledMessageTable.update({
        (ScheduledMessageTable.timestamp lessEq date) and
          (ScheduledMessageTable.isSent eq false) and
          (ScheduledMessageTable.isDeleted eq false)
      }) {
        it[isSent] = true
      }
    }
}
