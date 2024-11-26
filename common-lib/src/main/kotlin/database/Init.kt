package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.database.tables.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.SchemaUtils.drop
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction

/** @param args Url, driver, user, password */
fun main(args: Array<String>) {
  Database.connect(
    args[0],
    args[1],
    args[2],
    args[3],
  )

  transaction {
    addLogger(StdOutSqlLogger)
    drop(
      AssessmentTable,
      AssignmentTable,
      CourseStudents,
      CourseTable,
      ProblemTable,
      SolutionTable,
      StudentTable,
      TeacherTable,
      AdminTable,
      ParentTable,
    )
    create(
      AssessmentTable,
      AssignmentTable,
      CourseStudents,
      CourseTable,
      ProblemTable,
      SolutionTable,
      StudentTable,
      TeacherTable,
      AdminTable,
      ParentTable,
    )
  }
}
