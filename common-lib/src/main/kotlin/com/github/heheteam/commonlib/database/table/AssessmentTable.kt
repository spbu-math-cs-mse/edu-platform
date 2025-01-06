package com.github.heheteam.commonlib.database.table

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object AssessmentTable : LongIdTable("assessment") {
  val solutionId = reference("solutionId", SolutionTable.id)
  val teacherId = reference("teacherId", TeacherTable.id)
  val grade = integer("grade")
  val comment = text("comment")
  val timestamp = datetime("timestamp").defaultExpression(CurrentDateTime)
}
