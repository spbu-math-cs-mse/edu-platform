package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.database.table.AdminTable
import com.github.heheteam.commonlib.database.table.AssessmentTable
import com.github.heheteam.commonlib.database.table.AssignmentTable
import com.github.heheteam.commonlib.database.table.CourseStudents
import com.github.heheteam.commonlib.database.table.CourseTable
import com.github.heheteam.commonlib.database.table.CourseTeachers
import com.github.heheteam.commonlib.database.table.ParentStudents
import com.github.heheteam.commonlib.database.table.ParentTable
import com.github.heheteam.commonlib.database.table.ProblemTable
import com.github.heheteam.commonlib.database.table.SolutionGroupMessagesTable
import com.github.heheteam.commonlib.database.table.SolutionPersonalMessagesTable
import com.github.heheteam.commonlib.database.table.SolutionTable
import com.github.heheteam.commonlib.database.table.StudentTable
import com.github.heheteam.commonlib.database.table.TeacherTable
import com.github.heheteam.commonlib.loadConfig
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.SchemaUtils.drop
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.Transaction
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
    ProblemTable,
    SolutionTable,
    StudentTable,
    TeacherTable,
    AdminTable,
    ParentTable,
    SolutionGroupMessagesTable,
    SolutionPersonalMessagesTable,
  )

fun main() {
  val config = loadConfig().databaseConfig
  val database = Database.connect(config.url, config.driver, config.login, config.password)

  transaction {
    addLogger(StdOutSqlLogger)
    reset(database)
    fillWithMockData()
  }
}

fun reset(database: Database) {
  transaction(database) {
    drop(*allTables)
    create(*allTables)
  }
}

class MissingDataException(name: String, location: String) :
  RuntimeException("$name is missing in $location")

fun Transaction.fillWithMockData() {
  exec(
    object {}.javaClass.getClassLoader().getResource("mock_data.sql")?.readText(Charsets.UTF_8)
      ?: throw MissingDataException("mock_data.sql", "resources")
  )
}
