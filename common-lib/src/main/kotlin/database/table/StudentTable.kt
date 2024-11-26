package com.github.heheteam.commonlib.database.tables

import org.jetbrains.exposed.dao.id.IntIdTable

object StudentTable : IntIdTable("student") {
  val tgId = long("tgId")
}
