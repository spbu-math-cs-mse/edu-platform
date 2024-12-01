package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.Teacher
import com.github.heheteam.commonlib.api.*
import com.github.heheteam.commonlib.database.tables.ParentStudents
import com.github.heheteam.commonlib.database.tables.StudentTable
import com.github.heheteam.commonlib.database.tables.TeacherTable
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

  override fun resolveTeacher(teacherId: TeacherId): Teacher? =
    transaction(database) {
      StudentTable
        .selectAll()
        .where(StudentTable.id eq teacherId.id)
        .map {
          Teacher(
            teacherId,
          )
        }.singleOrNull()
    }
}
