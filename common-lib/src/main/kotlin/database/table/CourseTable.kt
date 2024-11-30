package com.github.heheteam.commonlib.database.tables

import org.jetbrains.exposed.dao.id.LongIdTable

object CourseTable : LongIdTable("course") {
  val description = varchar("description", 255)
}
