package com.github.heheteam.commonlib.database.tables

import org.jetbrains.exposed.dao.id.LongIdTable

object CourseTable : LongIdTable("course") {
  val name = varchar("name", 255)
}
