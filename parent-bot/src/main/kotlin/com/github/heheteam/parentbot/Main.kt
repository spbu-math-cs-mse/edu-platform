package com.github.heheteam.parentbot

import com.github.heheteam.commonlib.api.ParentApi
import com.github.heheteam.commonlib.database.DatabaseGradeTable
import com.github.heheteam.commonlib.database.DatabaseStudentStorage
import com.github.heheteam.commonlib.loadConfig
import com.github.heheteam.commonlib.mock.MockParentStorage
import org.jetbrains.exposed.sql.Database

/** @param args bot token and telegram @username for mocking data. */
suspend fun main(vararg args: String) {
  val botToken = args.first()
  val config = loadConfig()

  val database =
    Database.connect(
      config.databaseConfig.url,
      config.databaseConfig.driver,
      config.databaseConfig.login,
      config.databaseConfig.password,
    )

  val parentStorage = MockParentStorage()
  val core =
    ParentApi(DatabaseStudentStorage(database), DatabaseGradeTable(database), parentStorage)

  parentRun(botToken, core)
}
