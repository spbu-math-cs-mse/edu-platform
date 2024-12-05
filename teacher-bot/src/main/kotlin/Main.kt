package com.github.heheteam.teacherbot

import DatabaseCoursesDistributor
import com.github.heheteam.commonlib.database.DatabaseGradeTable
import com.github.heheteam.commonlib.database.DatabaseSolutionDistributor
import com.github.heheteam.commonlib.mock.*
import com.github.heheteam.teacherbot.run.teacherRun
import dev.inmo.tgbotapi.utils.RiskFeature
import org.jetbrains.exposed.sql.Database

/**
 * @param args bot token and telegram @username for mocking data.
 */
@OptIn(RiskFeature::class)
suspend fun main(vararg args: String) {
  val botToken = args.first()

  val database = Database.connect(
    "jdbc:h2:./data/films",
    driver = "org.h2.Driver",
  )

  val coursesDistributor = DatabaseCoursesDistributor(database)
  val inMemoryTeacherStatistics = InMemoryTeacherStatistics()

  val userIdRegistry = MockTeacherIdRegistry(0L)

  val core =
    TeacherCore(
      inMemoryTeacherStatistics,
      coursesDistributor,
      DatabaseSolutionDistributor(database),
      DatabaseGradeTable(database),
    )

  teacherRun(botToken, userIdRegistry, core)
}
