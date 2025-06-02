package com.github.heheteam.commonlib.database.table

import com.github.heheteam.commonlib.TelegramMessageContent
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.json.json
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

private const val SHORT_NAME_LENGTH = 255

object ScheduledMessageTable : LongIdTable("scheduled_message") {
  val timestamp = datetime("timestamp")
  val content = json<TelegramMessageContent>("content", Json)
  val shortName = varchar("short_name", SHORT_NAME_LENGTH)
  val courseId = long("course_id").references(CourseTable.id)
  val isSent = bool("is_sent").default(false)
  val isDeleted = bool("is_deleted").default(false)
  val adminId = long("admin_id")
}
