package com.github.heheteam.adminbot

import DatabaseCoursesDistributor
import com.github.heheteam.adminbot.run.adminRun
import com.github.heheteam.commonlib.database.DatabaseAssignmentStorage
import com.github.heheteam.commonlib.database.DatabaseProblemStorage
import com.github.heheteam.commonlib.database.DatabaseStudentStorage
import com.github.heheteam.commonlib.database.DatabaseTeacherStorage
import com.github.heheteam.commonlib.mock.InMemoryScheduledMessagesDistributor
import com.github.heheteam.commonlib.mock.MockAdminIdRegistry
import dev.inmo.tgbotapi.utils.RiskFeature
import org.jetbrains.exposed.sql.Database

@OptIn(RiskFeature::class)
suspend fun main(vararg args: String) {
  val botToken = args.first()

  val database =
    Database.connect(
      "jdbc:h2:./data/films",
      driver = "org.h2.Driver",
    )

  val userIdRegistry = MockAdminIdRegistry(0L)

  val core =
    AdminCore(
      InMemoryScheduledMessagesDistributor(),
      DatabaseCoursesDistributor(database),
      DatabaseStudentStorage(database),
      DatabaseTeacherStorage(database),
      DatabaseAssignmentStorage(database),
      DatabaseProblemStorage(database),
    )

  adminRun(botToken, userIdRegistry, core)
}
