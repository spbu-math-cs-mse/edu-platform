package com.github.heheteam.commonlib.database.table

import org.jetbrains.exposed.dao.id.LongIdTable

object StudentTable : LongIdTable("student") {
  val name = varchar("name", 255)
  val surname = varchar("surname", 255)
  val tgId = long("tgId")
}
