package com.github.heheteam.commonlib.database.table

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ReferenceOption

object ChallengeAccessTable : LongIdTable("challengeAccess") {
  val studentId = reference("studentId", StudentTable.id)
  val challengeId = reference("challengeId", AssignmentTable.id, onDelete = ReferenceOption.CASCADE)
}
