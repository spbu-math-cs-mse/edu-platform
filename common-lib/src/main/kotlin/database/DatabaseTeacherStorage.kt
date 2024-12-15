package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.Teacher
import com.github.heheteam.commonlib.api.*
import com.github.heheteam.commonlib.database.tables.ParentStudents
import com.github.heheteam.commonlib.database.tables.StudentTable
import com.github.heheteam.commonlib.database.tables.TeacherTable
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class DatabaseTeacherStorage(
  val database: Database,
) : TeacherStorage {
  init {
    transaction(database) {
      SchemaUtils.create(TeacherTable)
      SchemaUtils.create(StudentTable)
      SchemaUtils.create(ParentStudents)
    }
  }


  override fun createTeacher(): TeacherId =
    transaction(database) {
      TeacherTable.insert {
        it[TeacherTable.name] = "defaultName"
        it[TeacherTable.surname] = "defaultSurname"
        it[TeacherTable.tgId] = 0L
      } get TeacherTable.id
    }.value.toTeacherId()

  override fun resolveTeacher(teacherId: TeacherId): Result<Teacher, ResolveError<TeacherId>> =
    transaction(database) {
      val row = TeacherTable
        .selectAll()
        .where(TeacherTable.id eq teacherId.id)
        .singleOrNull() ?: return@transaction Err(ResolveError(teacherId))
      Ok(
        Teacher(
          teacherId,
          row[TeacherTable.name],
          row[TeacherTable.surname],
        ),
      )
    }
}
