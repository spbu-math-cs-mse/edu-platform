package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.api.MenuMessageInfo
import com.github.heheteam.commonlib.api.SolutionDistributor
import com.github.heheteam.commonlib.api.SolutionId
import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.api.TelegramMessageInfo
import com.github.heheteam.commonlib.api.TelegramTechnicalMessagesStorage
import com.github.heheteam.commonlib.database.table.SolutionGroupMessagesTable
import com.github.heheteam.commonlib.database.table.SolutionPersonalMessagesTable
import com.github.heheteam.commonlib.database.table.TeacherMenuMessageTable
import com.github.heheteam.commonlib.database.table.TeacherTable
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.get
import com.github.michaelbull.result.toResultOr
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.toChatId
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class DatabaseTelegramTechnicalMessagesStorage(
  val database: Database,
  val solutionDistributor: SolutionDistributor,
) : TelegramTechnicalMessagesStorage {
  init {
    transaction(database) {
      SchemaUtils.create(SolutionGroupMessagesTable)
      SchemaUtils.create(SolutionPersonalMessagesTable)
      SchemaUtils.create(TeacherMenuMessageTable)
    }
  }

  override fun registerGroupSolutionPublication(
    solutionId: SolutionId,
    telegramMessageInfo: TelegramMessageInfo,
  ) {
    transaction(database) {
      SolutionGroupMessagesTable.insert {
        it[SolutionGroupMessagesTable.solutionId] = solutionId.id
        it[messageId] = telegramMessageInfo.messageId.long
        it[chatId] = telegramMessageInfo.chatId.long
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
        it[messageId] = telegramMessageInfo.messageId.long
        it[chatId] = telegramMessageInfo.chatId.long
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

  override fun updateTeacherMenuMessage(telegramMessageInfo: TelegramMessageInfo) {
    transaction(database) {
      val rowsUpdated =
        TeacherMenuMessageTable.update({
          TeacherMenuMessageTable.chatId eq telegramMessageInfo.chatId.long
        }) {
          it[messageId] = telegramMessageInfo.messageId.long
          it[chatId] = telegramMessageInfo.chatId.long
        }
      if (rowsUpdated == 0) {
        TeacherMenuMessageTable.insert {
          it[messageId] = telegramMessageInfo.messageId.long
          it[chatId] = telegramMessageInfo.chatId.long
        }
      }
    }
  }

  override fun resolveTeacherMenuMessage(
    teacherId: TeacherId
  ): Result<List<TelegramMessageInfo>, String> {
    val row =
      transaction(database) {
          TeacherTable.join(
              TeacherMenuMessageTable,
              joinType = JoinType.INNER,
              onColumn = TeacherTable.tgId,
              otherColumn = TeacherMenuMessageTable.chatId,
            )
            .selectAll()
            .where(TeacherTable.id eq teacherId.id)
            .map {
              TelegramMessageInfo(
                RawChatId(it[TeacherMenuMessageTable.chatId]),
                MessageId(it[TeacherMenuMessageTable.messageId]),
              )
            }
        }
        .ifEmpty { null }
    return row.toResultOr { "" }
  }

  override fun resolveTeacherChatId(teacherId: TeacherId): Result<RawChatId, String> {
    val row =
      transaction(database) {
        TeacherTable.selectAll()
          .where(TeacherTable.id eq teacherId.id)
          .map { it[TeacherTable.tgId].toChatId().chatId }
          .firstOrNull()
      }
    return row.toResultOr { "" }
  }

  override fun resolveTeacherFirstUncheckedSolutionMessage(
    teacherId: TeacherId
  ): Result<MenuMessageInfo, String> {
    val row =
      transaction(database) {
        val solution = solutionDistributor.querySolution(teacherId).get()

        if (solution == null) {
          val chatId =
            TeacherTable.selectAll()
              .where(TeacherTable.id eq teacherId.id)
              .map { it[TeacherTable.tgId] }
              .firstOrNull() ?: return@transaction null
          return@transaction MenuMessageInfo(RawChatId(chatId))
        }

        return@transaction SolutionPersonalMessagesTable.selectAll()
          .where(SolutionPersonalMessagesTable.solutionId eq solution.id.id)
          .map {
            MenuMessageInfo(
              RawChatId(it[SolutionPersonalMessagesTable.chatId]),
              MessageId(it[SolutionPersonalMessagesTable.messageId]),
            )
          }
          .firstOrNull()
      }
    return row.toResultOr { "" }
  }
}
