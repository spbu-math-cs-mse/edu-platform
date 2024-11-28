package com.github.heheteam.commonlib.database.tables

import org.jetbrains.exposed.dao.id.LongIdTable

object ParentStudents : LongIdTable("parentStudents") {
  val parentId = reference("parentId", ParentTable.id)
  val studentId = reference("studentId", StudentTable.id)
}