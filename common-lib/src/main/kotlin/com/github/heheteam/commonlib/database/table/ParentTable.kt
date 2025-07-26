package com.github.heheteam.commonlib.database.table

import org.jetbrains.exposed.dao.id.LongIdTable

object ParentTable : LongIdTable("parent") {
  val name = varchar("name", 255)
  val surname = varchar("surname", 255)
  val tgId = long("tgId")
  val discoverySource = varchar("discovery_source", 255).nullable().default(null)
  val lastQuestState = varchar("lastQuestState", 255).nullable().default(null)
}
