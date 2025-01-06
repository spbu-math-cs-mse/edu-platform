package com.github.heheteam.commonlib.database.table

import org.jetbrains.exposed.dao.id.LongIdTable

object ParentTable : LongIdTable("parent") {
  val tgId = long("tgId")
  val name = varchar("name", 255)
  val surname = varchar("surname", 255)
}
