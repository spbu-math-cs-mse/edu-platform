package com.github.heheteam.commonlib.database.table

import com.github.heheteam.commonlib.TextWithMediaAttachments
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.json.json
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object AssessmentTable : LongIdTable("assessment") {
  val solutionId = reference("solutionId", SolutionTable.id)
  val teacherId = reference("teacherId", TeacherTable.id)
  val grade = integer("grade")
  val comment = json<TextWithMediaAttachments>("content", Json)
  val timestamp = datetime("timestamp").defaultExpression(CurrentDateTime)
}
