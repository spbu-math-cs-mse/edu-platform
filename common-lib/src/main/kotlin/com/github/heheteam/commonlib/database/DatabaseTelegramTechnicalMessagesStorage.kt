package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.EduPlatformError
import com.github.heheteam.commonlib.MenuMessageInfo
import com.github.heheteam.commonlib.TelegramMessageInfo
import com.github.heheteam.commonlib.asNamedError
import com.github.heheteam.commonlib.database.table.CourseTable
import com.github.heheteam.commonlib.database.table.SubmissionGroupMessagesTable
import com.github.heheteam.commonlib.database.table.SubmissionPersonalMessagesTable
import com.github.heheteam.commonlib.database.table.TeacherMenuMessageTable
import com.github.heheteam.commonlib.database.table.TeacherTable
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.SubmissionDistributor
import com.github.heheteam.commonlib.interfaces.SubmissionId
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.heheteam.commonlib.interfaces.TelegramTechnicalMessagesStorage
import com.github.heheteam.commonlib.util.ok
import com.github.michaelbull.result.Err
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
  val submissionDistributor: SubmissionDistributor,
) : TelegramTechnicalMessagesStorage {
  init {
    transaction(database) {
      SchemaUtils.create(SubmissionGroupMessagesTable)
      SchemaUtils.create(SubmissionPersonalMessagesTable)
      SchemaUtils.create(TeacherMenuMessageTable)
    }
  }

  override fun registerGroupSubmissionPublication(
    submissionId: SubmissionId,
    telegramMessageInfo: TelegramMessageInfo,
  ) {
    transaction(database) {
      SubmissionGroupMessagesTable.insert {
        it[SubmissionGroupMessagesTable.submissionId] = submissionId.long
        it[messageId] = telegramMessageInfo.messageId.long
        it[chatId] = telegramMessageInfo.chatId.long
      }
    }
  }

  override fun registerPersonalSubmissionPublication(
    submissionId: SubmissionId,
    telegramMessageInfo: TelegramMessageInfo,
  ) {
    transaction(database) {
      SubmissionPersonalMessagesTable.insert {
        it[SubmissionPersonalMessagesTable.submissionId] = submissionId.long
        it[messageId] = telegramMessageInfo.messageId.long
        it[chatId] = telegramMessageInfo.chatId.long
      }
    }
  }

  override fun resolveGroupMessage(
    submissionId: SubmissionId
  ): Result<TelegramMessageInfo, EduPlatformError> {
    val row =
      transaction(database) {
        SubmissionGroupMessagesTable.selectAll()
          .where(SubmissionGroupMessagesTable.submissionId eq submissionId.long)
          .map {
            TelegramMessageInfo(
              RawChatId(it[SubmissionGroupMessagesTable.chatId]),
              MessageId(it[SubmissionGroupMessagesTable.messageId]),
            )
          }
      }
    return row.singleOrNull().toResultOr {
      "Failed to resolve a singlie group message (got ${row.size})".asNamedError()
    }
  }

  override fun resolvePersonalMessage(
    submissionId: SubmissionId
  ): Result<TelegramMessageInfo, EduPlatformError> {
    val row =
      transaction(database) {
        SubmissionPersonalMessagesTable.selectAll()
          .where(SubmissionPersonalMessagesTable.submissionId eq submissionId.long)
          .map {
            TelegramMessageInfo(
              RawChatId(it[SubmissionPersonalMessagesTable.chatId]),
              MessageId(it[SubmissionPersonalMessagesTable.messageId]),
            )
          }
      }
    return row.singleOrNull().toResultOr {
      "Failed to resolve a singlie personal message (got ${row.size})".asNamedError()
    }
  }

  override fun updateTeacherMenuMessage(
    telegramMessageInfo: TelegramMessageInfo
  ): Result<Unit, EduPlatformError> {
    return transaction(database) {
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
      .ok()
  }

  override fun resolveTeacherMenuMessage(
    teacherId: TeacherId
  ): Result<List<TelegramMessageInfo>, EduPlatformError> {
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
    return row.toResultOr { "Failed to lookup teacher menu message".asNamedError() }
  }

  override fun resolveTeacherFirstUncheckedSubmissionMessage(
    teacherId: TeacherId
  ): Result<MenuMessageInfo, EduPlatformError> {
    return transaction(database) {
      val submission = submissionDistributor.querySubmission(teacherId).get()

      if (submission == null) {
        val chatId =
          TeacherTable.selectAll()
            .where(TeacherTable.id eq teacherId.long)
            .map { it[TeacherTable.tgId] }
            .firstOrNull()
            ?: return@transaction Err("No tg id for the teacher $teacherId".asNamedError())
        return@transaction MenuMessageInfo(RawChatId(chatId)).ok()
      }

      return@transaction SubmissionPersonalMessagesTable.selectAll()
        .where(SubmissionPersonalMessagesTable.submissionId eq submission.id.long)
        .map {
          MenuMessageInfo(
            RawChatId(it[SubmissionPersonalMessagesTable.chatId]),
            MessageId(it[SubmissionPersonalMessagesTable.messageId]),
          )
        }
        .firstOrNull()
        .toResultOr {
          "Submission ${submission.id} does not have a corresponding message".asNamedError()
        }
    }
  }

  override fun resolveGroupMenuMessage(
    courseId: CourseId
  ): Result<List<TelegramMessageInfo>, EduPlatformError> {
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
    return row.toResultOr { "Failed to resolve group menu messages".asNamedError() }
  }

  override fun resolveGroupFirstUncheckedSubmissionMessage(
    courseId: CourseId
  ): Result<MenuMessageInfo, EduPlatformError> {
    return transaction(database) {
      val submission = submissionDistributor.querySubmission(courseId).get()
      if (submission == null) {
        val chatId =
          CourseTable.selectAll()
            .where(CourseTable.id eq courseId.long)
            .map { it[CourseTable.groupRawChatId] }
            .firstOrNull()
            ?: return@transaction Err(
              "Course $courseId does not have a corresponding group!".asNamedError()
            )
        return@transaction MenuMessageInfo(RawChatId(chatId)).ok()
      }

      return@transaction SubmissionGroupMessagesTable.selectAll()
        .where(SubmissionGroupMessagesTable.submissionId eq submission.id.long)
        .map {
          MenuMessageInfo(
            RawChatId(it[SubmissionGroupMessagesTable.chatId]),
            MessageId(it[SubmissionGroupMessagesTable.messageId]),
          )
        }
        .firstOrNull()
        .toResultOr {
          "The submission ${submission.id} does not have an associated message in a group of the course $courseId"
            .asNamedError()
        }
    }
  }
}
