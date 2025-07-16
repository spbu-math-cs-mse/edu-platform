package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.database.table.ParentStudents
import com.github.heheteam.commonlib.database.table.ParentTable
import com.github.heheteam.commonlib.domain.RichParent
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.heheteam.commonlib.interfaces.ParentId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.repository.ParentRepository
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import dev.inmo.tgbotapi.types.RawChatId
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

class DatabaseParentRepository : ParentRepository {
  override fun save(richParent: RichParent): Result<RichParent, EduPlatformError> = binding {
    val parentIdLong = richParent.id.long
    val exists = ParentTable.selectAll().where { ParentTable.id eq parentIdLong }.count() > 0

    if (exists) {
      ParentTable.update({ ParentTable.id eq parentIdLong }) {
        it[ParentTable.name] = richParent.firstName
        it[ParentTable.surname] = richParent.lastName
        it[ParentTable.tgId] = richParent.tgId.long
        it[ParentTable.discoverySource] = richParent.from
        it[ParentTable.lastQuestState] = richParent.lastQuestState
      }
    } else {
      ParentTable.insert {
        it[ParentTable.id] = parentIdLong
        it[ParentTable.name] = richParent.firstName
        it[ParentTable.surname] = richParent.lastName
        it[ParentTable.tgId] = richParent.tgId.long
        it[ParentTable.lastQuestState] = richParent.lastQuestState
        it[ParentTable.discoverySource] = richParent.from
      }
    }

    // Synchronize students
    ParentStudents.deleteWhere { ParentStudents.parentId eq parentIdLong }
    if (richParent.children.isNotEmpty()) {
      ParentStudents.batchInsert(richParent.children) { studentId ->
        this[ParentStudents.parentId] = parentIdLong
        this[ParentStudents.studentId] = studentId.long
      }
    }

    richParent
  }

  override fun findById(parentId: ParentId): Result<RichParent?, EduPlatformError> {
    return ParentTable.selectAll()
      .where { ParentTable.id eq parentId.long }
      .singleOrNull()
      ?.let { row ->
        val selectAll = ParentStudents.selectAll()
        val children =
          selectAll
            .where { ParentStudents.parentId eq parentId.long }
            .map { StudentId(it[ParentStudents.studentId].value) }
            .toMutableList()

        Ok(
          RichParent(
            id = ParentId(row[ParentTable.id].value),
            firstName = row[ParentTable.name],
            lastName = row[ParentTable.surname],
            tgId = RawChatId(row[ParentTable.tgId]),
            lastQuestState = row[ParentTable.lastQuestState],
            from = row[ParentTable.discoverySource],
            children = children,
          )
        )
      } ?: Ok(null)
  }

  override fun findByTgId(tgId: RawChatId): Result<RichParent?, EduPlatformError> {
    return ParentTable.selectAll()
      .where { ParentTable.tgId eq tgId.long }
      .singleOrNull()
      ?.let { row ->
        val parentId = ParentId(row[ParentTable.id].value)
        val children =
          ParentStudents.selectAll()
            .where { ParentStudents.parentId eq parentId.long }
            .map { StudentId(it[ParentStudents.studentId].value) }
            .toMutableList()

        Ok(
          RichParent(
            id = parentId,
            firstName = row[ParentTable.name],
            lastName = row[ParentTable.surname],
            tgId = RawChatId(row[ParentTable.tgId]),
            lastQuestState = row[ParentTable.lastQuestState],
            from = row[ParentTable.discoverySource],
            children = children,
          )
        )
      } ?: Ok(null)
  }
}
