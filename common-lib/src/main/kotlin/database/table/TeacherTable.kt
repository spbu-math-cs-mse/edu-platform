package com.github.heheteam.commonlib.database.tables

import org.jetbrains.exposed.dao.id.LongIdTable

object TeacherTable : LongIdTable("teacher") {
    val name = varchar("name", 255)
    val surname = varchar("surname", 255)
    val tgId = long("tgId")
}
