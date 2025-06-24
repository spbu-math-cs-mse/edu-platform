package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.SentMessageLog
import com.github.heheteam.commonlib.database.table.SentMessageLogTable
import com.github.heheteam.commonlib.errors.DatabaseExceptionError
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.heheteam.commonlib.interfaces.ScheduledMessageId
import com.github.heheteam.commonlib.interfaces.SentMessageLogStorage
import com.github.heheteam.commonlib.interfaces.toScheduledMessageId
import com.github.heheteam.commonlib.interfaces.toStudentId
import com.github.heheteam.commonlib.util.catchingTransaction
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class DatabaseSentMessageLogStorage(private val database: Database) : SentMessageLogStorage {
  override fun logSentMessage(log: SentMessageLog): Result<Unit, EduPlatformError> =
    transaction(database) {
      runCatching {
          SentMessageLogTable.insert {
            it[scheduledMessageId] = log.scheduledMessageId.long
            it[studentId] = log.studentId.long
            it[sentTimestamp] = log.sentTimestamp
            it[telegramMessageId] = log.telegramMessageId.long
            it[chatId] = log.chatId.long
          }
          Unit
        }
        .mapError { DatabaseExceptionError(it) }
    }

  override fun getSentMessageLogs(
    scheduledMessageId: ScheduledMessageId
  ): Result<List<SentMessageLog>, EduPlatformError> =
    catchingTransaction(database) {
      SentMessageLogTable.selectAll()
        .where { SentMessageLogTable.scheduledMessageId eq scheduledMessageId.long }
        .map {
          SentMessageLog(
            logId = it[SentMessageLogTable.id].value,
            scheduledMessageId = it[SentMessageLogTable.scheduledMessageId].toScheduledMessageId(),
            studentId = it[SentMessageLogTable.studentId].toStudentId(),
            sentTimestamp = it[SentMessageLogTable.sentTimestamp],
            telegramMessageId = MessageId(it[SentMessageLogTable.telegramMessageId]),
            chatId = RawChatId(it[SentMessageLogTable.chatId]),
          )
        }
    }
}
