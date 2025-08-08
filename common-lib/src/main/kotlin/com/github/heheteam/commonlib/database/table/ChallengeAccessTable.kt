package com.github.heheteam.commonlib.database.table

import org.jetbrains.exposed.dao.id.LongIdTable

object ChallengeAccessTable : LongIdTable("challenge") {
  val studentId = reference("studentId", StudentTable.id)
  val challengeId = reference("challengeId", AssignmentTable.id)
}
