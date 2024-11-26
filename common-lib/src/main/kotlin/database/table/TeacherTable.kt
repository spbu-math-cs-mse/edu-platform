package com.github.heheteam.commonlib.database.tables

import org.jetbrains.exposed.dao.id.IntIdTable

object TeacherTable : IntIdTable("student") {
  val tgId = long("tgId")
}
