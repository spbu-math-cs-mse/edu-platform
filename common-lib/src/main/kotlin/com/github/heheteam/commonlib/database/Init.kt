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
import com.github.heheteam.commonlib.database.table.TeacherMenuMessageTable
import com.github.heheteam.commonlib.database.table.TeacherTable
import com.github.heheteam.commonlib.loadConfig
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.SchemaUtils.drop
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
    ProblemTable,
    SolutionTable,
    StudentTable,
    TeacherTable,
    AdminTable,
    ParentTable,
    SolutionGroupMessagesTable,
    SolutionPersonalMessagesTable,
    TeacherMenuMessageTable,
  )

fun main() {
  val config = loadConfig().databaseConfig
  val database = Database.connect(config.url, config.driver, config.login, config.password)

  transaction {
    addLogger(StdOutSqlLogger)
    reset(database)
  }
}

fun reset(database: Database) {
  transaction(database) {
    exec("DROP TABLE IF EXISTS solution CASCADE")
    drop(*allTables)
    create(*allTables)
  }
}
