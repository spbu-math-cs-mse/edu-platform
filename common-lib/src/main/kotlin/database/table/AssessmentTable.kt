package com.github.heheteam.commonlib.database.tables

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object AssessmentTable : LongIdTable("assessment") {
  val solutionId = reference("solutionId", SolutionTable.id)
  val teacherId = reference("teacherId", TeacherTable.id)
  val grade = integer("grade")
  val timestamp = datetime("timestamp").defaultExpression(CurrentDateTime)
}
