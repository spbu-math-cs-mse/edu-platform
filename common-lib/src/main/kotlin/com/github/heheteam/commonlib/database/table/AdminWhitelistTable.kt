package com.github.heheteam.commonlib.database.table

import org.jetbrains.exposed.dao.id.LongIdTable

object AdminWhitelistTable : LongIdTable("adminWhitelist") {
  val tgId = long("tgId")
}
