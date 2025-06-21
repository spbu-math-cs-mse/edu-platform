package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.config.loadConfig
import com.github.heheteam.commonlib.database.table.AdminTable
import com.github.heheteam.commonlib.database.table.AdminWhitelistTable
import com.github.heheteam.commonlib.database.table.AssessmentTable
import com.github.heheteam.commonlib.database.table.AssignmentTable
import com.github.heheteam.commonlib.database.table.CourseStudents
import com.github.heheteam.commonlib.database.table.CourseTable
import com.github.heheteam.commonlib.database.table.CourseTeachers
import com.github.heheteam.commonlib.database.table.CourseTokenTable
import com.github.heheteam.commonlib.database.table.ParentStudents
import com.github.heheteam.commonlib.database.table.ParentTable
import com.github.heheteam.commonlib.database.table.PersonalDeadlineTable
import com.github.heheteam.commonlib.database.table.ProblemTable
import com.github.heheteam.commonlib.database.table.ScheduledMessageTable
import com.github.heheteam.commonlib.database.table.SentMessageLogTable
import com.github.heheteam.commonlib.database.table.StudentTable
import com.github.heheteam.commonlib.database.table.SubmissionGroupMessagesTable
import com.github.heheteam.commonlib.database.table.SubmissionPersonalMessagesTable
import com.github.heheteam.commonlib.database.table.SubmissionTable
import com.github.heheteam.commonlib.database.table.TeacherMenuMessageTable
import com.github.heheteam.commonlib.database.table.TeacherTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction

private val allTables =
  arrayOf(
    CourseStudents,
    CourseTeachers,
    ParentStudents,
    AssessmentTable,
    AssignmentTable,
    CourseTable,
    ScheduledMessageTable,
    ProblemTable,
    SubmissionTable,
    StudentTable,
    SentMessageLogTable,
    TeacherTable,
    AdminTable,
    ParentTable,
    SubmissionGroupMessagesTable,
    SubmissionPersonalMessagesTable,
    TeacherMenuMessageTable,
    PersonalDeadlineTable,
    CourseTokenTable,
    AdminWhitelistTable,
  )

fun main(args: Array<String>) {
  val config = loadConfig().databaseConfig
  val database = Database.connect(config.url, config.driver, config.login, config.password)

  transaction {
    addLogger(StdOutSqlLogger)
    reset(database)
  }
}

fun reset(database: Database) {
  transaction(database) {
    when (database.vendor) {
      "H2" -> exec("DROP ALL OBJECTS")
      else -> exec("DROP TABLE IF EXISTS ${allTables.joinToString { it.tableName }} CASCADE")
    }
    SchemaUtils.create(*allTables)
  }
}
