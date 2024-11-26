package com.github.heheteam.commonlib.database.tables

import org.jetbrains.exposed.dao.id.LongIdTable

object ParentTable : LongIdTable("parent") {
  val tgId = long("tgId")
}
