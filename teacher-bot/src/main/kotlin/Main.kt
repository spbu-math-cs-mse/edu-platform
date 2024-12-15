package com.github.heheteam.teacherbot

import DatabaseCoursesDistributor
import com.github.heheteam.commonlib.api.RedisBotEventBus
import com.github.heheteam.commonlib.database.DatabaseGradeTable
import com.github.heheteam.commonlib.database.DatabaseProblemStorage
import com.github.heheteam.commonlib.database.DatabaseSolutionDistributor
import com.github.heheteam.commonlib.database.DatabaseTeacherStorage
import com.github.heheteam.commonlib.mock.*
import com.github.heheteam.teacherbot.run.teacherRun
import org.jetbrains.exposed.sql.Database

/**
 * @param args bot token and telegram @username for mocking data.
 */
suspend fun main(vararg args: String) {
  val botToken = args.first()

  val database = Database.connect(
    "jdbc:h2:./data/films",
    driver = "org.h2.Driver",
  )

  val coursesDistributor = DatabaseCoursesDistributor(database)
  val inMemoryTeacherStatistics = InMemoryTeacherStatistics()

  val userIdRegistry = MockTeacherIdRegistry(0L)
  val teacherStorage = DatabaseTeacherStorage(database)
  val botEventBus = RedisBotEventBus()

  val core =
    TeacherCore(
      inMemoryTeacherStatistics,
      coursesDistributor,
      DatabaseSolutionDistributor(database),
      DatabaseGradeTable(database),
      DatabaseProblemStorage(database),
      botEventBus,
    )

  teacherRun(botToken, userIdRegistry, teacherStorage, core)
}
