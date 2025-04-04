package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.MenuMessageInfo
import com.github.heheteam.commonlib.TelegramMessageInfo
import com.github.heheteam.commonlib.database.table.CourseTable
import com.github.heheteam.commonlib.database.table.SolutionGroupMessagesTable
import com.github.heheteam.commonlib.database.table.SolutionPersonalMessagesTable
import com.github.heheteam.commonlib.database.table.TeacherMenuMessageTable
import com.github.heheteam.commonlib.database.table.TeacherTable
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.SolutionDistributor
import com.github.heheteam.commonlib.interfaces.SolutionId
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.heheteam.commonlib.interfaces.TelegramTechnicalMessagesStorage
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.get
import com.github.michaelbull.result.toResultOr
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

internal class DatabaseTelegramTechnicalMessagesStorage(
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
        it[SolutionGroupMessagesTable.solutionId] = solutionId.long
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
        it[SolutionPersonalMessagesTable.solutionId] = solutionId.long
        it[messageId] = telegramMessageInfo.messageId.long
        it[chatId] = telegramMessageInfo.chatId.long
      }
    }
  }

  override fun resolveGroupMessage(solutionId: SolutionId): Result<TelegramMessageInfo, String> {
    val row =
      transaction(database) {
        SolutionGroupMessagesTable.selectAll()
          .where(SolutionGroupMessagesTable.solutionId eq solutionId.long)
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
          .where(SolutionPersonalMessagesTable.solutionId eq solutionId.long)
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
          .where(TeacherTable.id eq teacherId.long)
          .map {
            TelegramMessageInfo(
              RawChatId(it[TeacherMenuMessageTable.chatId]),
              MessageId(it[TeacherMenuMessageTable.messageId]),
            )
          }
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
              .where(TeacherTable.id eq teacherId.long)
              .map { it[TeacherTable.tgId] }
              .firstOrNull() ?: return@transaction null
          return@transaction MenuMessageInfo(RawChatId(chatId))
        }

        return@transaction SolutionPersonalMessagesTable.selectAll()
          .where(SolutionPersonalMessagesTable.solutionId eq solution.id.long)
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

  override fun resolveGroupMenuMessage(
    courseId: CourseId
  ): Result<List<TelegramMessageInfo>, String> {
    val row =
      transaction(database) {
        CourseTable.join(
            TeacherMenuMessageTable,
            joinType = JoinType.INNER,
            onColumn = CourseTable.groupRawChatId,
            otherColumn = TeacherMenuMessageTable.chatId,
          )
          .selectAll()
          .where(CourseTable.id eq courseId.long)
          .map {
            TelegramMessageInfo(
              RawChatId(it[TeacherMenuMessageTable.chatId]),
              MessageId(it[TeacherMenuMessageTable.messageId]),
            )
          }
      }
    return row.toResultOr { "failed to resolve group menu message" }
  }

  override fun resolveGroupFirstUncheckedSolutionMessage(
    courseId: CourseId
  ): Result<MenuMessageInfo, String> {
    val row =
      transaction(database) {
        val solution = solutionDistributor.querySolution(courseId).get()
        if (solution == null) {
          val chatId =
            CourseTable.selectAll()
              .where(CourseTable.id eq courseId.long)
              .map { it[CourseTable.groupRawChatId] }
              .firstOrNull() ?: return@transaction null
          return@transaction MenuMessageInfo(RawChatId(chatId))
        }

        return@transaction SolutionGroupMessagesTable.selectAll()
          .where(SolutionGroupMessagesTable.solutionId eq solution.id.long)
          .map {
            MenuMessageInfo(
              RawChatId(it[SolutionGroupMessagesTable.chatId]),
              MessageId(it[SolutionGroupMessagesTable.messageId]),
            )
          }
          .firstOrNull()
      }
    return row.toResultOr { "" }
  }
}
