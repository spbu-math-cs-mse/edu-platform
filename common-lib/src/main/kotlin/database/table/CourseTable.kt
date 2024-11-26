package com.github.heheteam.commonlib.database.tables

import org.jetbrains.exposed.dao.id.IntIdTable

object CourseTable : IntIdTable("course") {
  val description = varchar("description", 100)
}