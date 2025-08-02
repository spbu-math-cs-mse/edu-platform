package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.NewScheduledMessageInfo
import com.github.heheteam.commonlib.ScheduledMessage
import com.github.heheteam.commonlib.database.table.ScheduledMessageTable
import com.github.heheteam.commonlib.errors.DatabaseExceptionError
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.heheteam.commonlib.errors.MaybeEduPlatformError
import com.github.heheteam.commonlib.errors.ResolveError
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.ScheduledMessageId
import com.github.heheteam.commonlib.interfaces.toScheduledMessageId
import com.github.heheteam.commonlib.logic.UserGroup
import com.github.heheteam.commonlib.util.catchingTransaction
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class DatabaseScheduledMessagesStorage(private val database: Database) {
  fun storeScheduledMessage(
    adminId: AdminId,
    messageInfo: NewScheduledMessageInfo,
  ): Result<ScheduledMessageId, EduPlatformError> =
    transaction(database) {
      runCatching {
          val id =
            ScheduledMessageTable.insertAndGetId {
              it[timestamp] = messageInfo.timestamp
              it[content] = messageInfo.content
              it[shortName] = messageInfo.shortName
              it[isSent] = false
              it[isDeleted] = false
              it[this.adminId] = adminId.long
              it[userGroup] = messageInfo.sendingFilter
            }
          id.value.toScheduledMessageId()
        }
        .mapError { DatabaseExceptionError(it) }
    }

  fun resolveScheduledMessage(
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
        isSent = it[ScheduledMessageTable.isSent],
        isDeleted = it[ScheduledMessageTable.isDeleted],
        adminId = AdminId(it[ScheduledMessageTable.adminId]),
        userGroup = it[ScheduledMessageTable.userGroup],
      )
    } ?: Err(ResolveError(scheduledMessageId, "ScheduledMessage")).bind()
  }

  fun viewScheduledMessages(
    adminId: AdminId?,
    courseId: CourseId?,
    lastN: Int,
  ): Result<List<ScheduledMessage>, EduPlatformError> =
    catchingTransaction(database) {
        val query = ScheduledMessageTable.selectAll()
        adminId?.let { query.andWhere { ScheduledMessageTable.adminId eq it.long } }
        query.orderBy(ScheduledMessageTable.timestamp to SortOrder.DESC).limit(lastN).map {
          ScheduledMessage(
            id = it[ScheduledMessageTable.id].value.toScheduledMessageId(),
            timestamp = it[ScheduledMessageTable.timestamp],
            content = it[ScheduledMessageTable.content],
            shortName = it[ScheduledMessageTable.shortName],
            isSent = it[ScheduledMessageTable.isSent],
            isDeleted = it[ScheduledMessageTable.isDeleted],
            adminId = AdminId(it[ScheduledMessageTable.adminId]),
            userGroup = it[ScheduledMessageTable.userGroup],
          )
        }
      }
      .map { courses ->
        if (courseId != null) {
          courses.filter {
            it.userGroup is UserGroup.CourseGroup && it.userGroup.courseId == courseId
          }
        } else courses
      }

  fun setIsDeleted(scheduledMessageId: ScheduledMessageId): Result<Int, DatabaseExceptionError> =
    this.runCatching {
        transaction(database) {
          ScheduledMessageTable.update({ ScheduledMessageTable.id eq scheduledMessageId.long }) {
            it[isDeleted] = true
          }
        }
      }
      .mapError<Int, Throwable, DatabaseExceptionError> { DatabaseExceptionError(it) }

  fun getMessagesUpToDate(date: LocalDateTime): Result<List<ScheduledMessage>, EduPlatformError> {
    return catchingTransaction(database) {
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
            isSent = it[ScheduledMessageTable.isSent],
            isDeleted = it[ScheduledMessageTable.isDeleted],
            adminId = AdminId(it[ScheduledMessageTable.adminId]),
            userGroup = it[ScheduledMessageTable.userGroup],
          )
        }
    }
  }

  fun markMessagesUpToDateAsSent(date: LocalDateTime): MaybeEduPlatformError =
    catchingTransaction(database) {
      ScheduledMessageTable.update({
        (ScheduledMessageTable.timestamp lessEq date) and
          (ScheduledMessageTable.isSent eq false) and
          (ScheduledMessageTable.isDeleted eq false)
      }) {
        it[isSent] = true
      }
    }
}
