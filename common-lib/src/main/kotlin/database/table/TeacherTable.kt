package com.github.heheteam.commonlib.database.tables

import org.jetbrains.exposed.dao.id.LongIdTable

object TeacherTable : LongIdTable("teacher") {
  val tgId = long("tgId")
}
