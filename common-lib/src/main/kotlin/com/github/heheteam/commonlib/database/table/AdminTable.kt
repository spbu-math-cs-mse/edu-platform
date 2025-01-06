package com.github.heheteam.commonlib.database.table

import org.jetbrains.exposed.dao.id.LongIdTable

object AdminTable : LongIdTable("admin") {
  val tgId = long("tgId")
  val name = varchar("name", 255)
  val surname = varchar("surname", 255)
}
