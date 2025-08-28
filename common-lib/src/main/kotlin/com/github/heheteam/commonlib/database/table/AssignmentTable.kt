package com.github.heheteam.commonlib.database.table

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.not
import org.jetbrains.exposed.sql.or

object AssignmentTable : LongIdTable("assignment") {
  val serialNumber = integer("serialNumber")
  val description = varchar("description", 100)
  val courseId = reference("courseId", CourseTable.id, onDelete = ReferenceOption.CASCADE)
  val statementsUrl = varchar("statementsUrl", 100).nullable()
  val challengeId = reference("challengeId", AssignmentTable.id).nullable().default(null)
  val isChallenge = bool("isChallenge").default(false)

  init {
    check { (isChallenge and challengeId.isNull()) or not(isChallenge) }
  }
}
