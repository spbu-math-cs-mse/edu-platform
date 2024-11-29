package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.api.ParentId
import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.api.StudentStorage
import com.github.heheteam.commonlib.database.tables.StudentTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

class DatabaseStudentStorage(val database: Database) : StudentStorage {
  override fun bindStudentToParent(
    studentId: StudentId,
    parentId: ParentId,
  ) {
    TODO("Not yet implemented")
  }

  override fun getStudents(parentId: ParentId): List<Student> {
    TODO("Not yet implemented")
  }

  override fun createStudent(): StudentId {
    TODO("Does this not clone StudentIdRegistry? ^-")
    transaction(database) {
      StudentTable.insert { }
    }
  }
}
