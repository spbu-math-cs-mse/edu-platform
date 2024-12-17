package com.github.heheteam.adminbot

import DatabaseCoursesDistributor
import com.github.heheteam.adminbot.run.adminRun
import com.github.heheteam.commonlib.database.DatabaseAssignmentStorage
import com.github.heheteam.commonlib.database.DatabaseProblemStorage
import com.github.heheteam.commonlib.database.DatabaseStudentStorage
import com.github.heheteam.commonlib.database.DatabaseTeacherStorage
import com.github.heheteam.commonlib.mock.InMemoryScheduledMessagesDistributor
import com.github.heheteam.commonlib.util.fillWithSamples
import org.jetbrains.exposed.sql.Database

suspend fun main(vararg args: String) {
  val botToken = args.first()

  val database =
    Database.connect(
      "jdbc:h2:./data/films",
      driver = "org.h2.Driver",
    )

  val coursesDistributor = DatabaseCoursesDistributor(database)
  val problemStorage = DatabaseProblemStorage(database)
  val assignmentStorage = DatabaseAssignmentStorage(database)
  val studentStorage = DatabaseStudentStorage(database)
  val teacherStorage = DatabaseTeacherStorage(database)

  fillWithSamples(coursesDistributor, problemStorage, assignmentStorage, studentStorage, teacherStorage)

  val core =
    AdminCore(
      InMemoryScheduledMessagesDistributor(),
      coursesDistributor,
      studentStorage,
      teacherStorage,
      assignmentStorage,
      problemStorage,
    )

  adminRun(botToken, core)
}
