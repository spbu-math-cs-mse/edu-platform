package com.github.heheteam.commonlib.database.table

import com.github.heheteam.commonlib.api.SolutionId
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.toResultOr
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class DatabaseTelegramTechnicalMessagesStorage(val database: Database) :
  TelegramTechnicalMessagesStorage {
  init {
    transaction(database) { SchemaUtils.create(SolutionGroupMessagesTable) }
  }

  override fun registerGroupSolutionPublication(
    solutionId: SolutionId,
    telegramMessageInfo: TelegramMessageInfo,
  ) {
    transaction(database) {
      SolutionGroupMessagesTable.insert {
        it[SolutionGroupMessagesTable.solutionId] = solutionId.id
        it[SolutionGroupMessagesTable.messageId] = telegramMessageInfo.messageId.long
        it[SolutionGroupMessagesTable.chatId] = telegramMessageInfo.chatId.long
      }
    }
  }

  override fun registerPersonalSolutionPublication(
    solutionId: SolutionId,
    telegramMessageInfo: TelegramMessageInfo,
  ) {
    transaction(database) {
      SolutionPersonalMessagesTable.insert {
        it[SolutionPersonalMessagesTable.solutionId] = solutionId.id
        it[SolutionPersonalMessagesTable.messageId] = telegramMessageInfo.messageId.long
        it[SolutionPersonalMessagesTable.chatId] = telegramMessageInfo.chatId.long
      }
    }
  }

  override fun resolveGroupMessage(solutionId: SolutionId): Result<TelegramMessageInfo, String> {
    val row =
      transaction(database) {
        SolutionGroupMessagesTable.selectAll()
          .where(SolutionGroupMessagesTable.solutionId eq solutionId.id)
          .map {
            TelegramMessageInfo(
              RawChatId(it[SolutionGroupMessagesTable.chatId]),
              MessageId(it[SolutionGroupMessagesTable.messageId]),
            )
          }
      }
    return row.singleOrNull().toResultOr { "" }
  }

  override fun resolvePersonalMessage(solutionId: SolutionId): Result<TelegramMessageInfo, String> {
    val row =
      transaction(database) {
        SolutionPersonalMessagesTable.selectAll()
          .where(SolutionPersonalMessagesTable.solutionId eq solutionId.id)
          .map {
            TelegramMessageInfo(
              RawChatId(it[SolutionPersonalMessagesTable.chatId]),
              MessageId(it[SolutionPersonalMessagesTable.messageId]),
            )
          }
      }
    return row.singleOrNull().toResultOr { "" }
  }
}
