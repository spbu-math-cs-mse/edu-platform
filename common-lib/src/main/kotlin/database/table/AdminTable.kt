package com.github.heheteam.commonlib.database.tables

import org.jetbrains.exposed.dao.id.LongIdTable

object AdminTable : LongIdTable("admin") {
  val tgId = long("tgId")
}
