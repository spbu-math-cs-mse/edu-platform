package com.github.heheteam.commonlib.database.table

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object PersonalDeadlineTable : LongIdTable("personalDeadline") {
  val studentId = reference("studentId", StudentTable.id)

  /** The new deadline calculates as max(deadline, original deadline) */
  val deadline = datetime("deadline").nullable()
}
