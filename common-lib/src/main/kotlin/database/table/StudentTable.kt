package com.github.heheteam.commonlib.database.tables

import org.jetbrains.exposed.dao.id.LongIdTable

object StudentTable : LongIdTable("student") {
  val tgId = long("tgId")
}
