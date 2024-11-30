package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.api.ParentId
import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.api.StudentStorage
import com.github.heheteam.commonlib.api.toStudentId
import com.github.heheteam.commonlib.database.tables.ParentStudents
import com.github.heheteam.commonlib.database.tables.StudentTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class DatabaseStudentStorage(
  val database: Database,
) : StudentStorage {
  init {
    transaction(database) {
      SchemaUtils.create(StudentTable)
      SchemaUtils.create(ParentStudents)
    }
  }

  override fun bindStudentToParent(
    studentId: StudentId,
    parentId: ParentId,
  ) {
    transaction(database) {
      ParentStudents.insert {
        it[ParentStudents.studentId] = studentId.id
        it[ParentStudents.parentId] = parentId.id
      }
    }
  }

  override fun getChildren(parentId: ParentId): List<Student> {
    val ids =
      transaction(database) {
        ParentStudents.selectAll().where(ParentStudents.parentId eq parentId.id)
      }.map { it[ParentStudents.studentId].value }
    return ids.map { studentId ->
      transaction(database) {
        StudentTable
          .selectAll()
          .where(StudentTable.id eq studentId)
          .map {
            Student(
              studentId.toStudentId(),
              it[StudentTable.name],
              it[StudentTable.name],
            )
          }.single()
      }
    }
  }

  override fun createStudent(): StudentId =
    transaction(database) {
      StudentTable.insert {
        it[StudentTable.name] = "defaultName"
        it[StudentTable.surname] = "defaultSurname"
        it[StudentTable.tgId] = 0L
      } get StudentTable.id
    }.value.toStudentId()

  override fun resolveStudent(studentId: StudentId): Student? =
    transaction(database) {
      StudentTable
        .selectAll()
        .where(StudentTable.id eq studentId.id)
        .map {
          Student(
            studentId,
            it[StudentTable.name],
            it[StudentTable.name],
          )
        }.singleOrNull()
    }
}
