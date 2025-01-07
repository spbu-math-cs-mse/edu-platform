package com.github.heheteam.parentbot

import com.github.heheteam.commonlib.database.DatabaseGradeTable
import com.github.heheteam.commonlib.database.DatabaseStudentStorage
import com.github.heheteam.commonlib.loadConfig
import com.github.heheteam.commonlib.mock.MockParentStorage
import com.github.heheteam.parentbot.run.parentRun
import org.jetbrains.exposed.sql.Database

/**
 * @param args bot token and telegram @username for mocking data.
 */
suspend fun main(vararg args: String) {
  val botToken = args.first()
  val config = loadConfig()

  val database = Database.connect(
    config.databaseConfig.url,
    config.databaseConfig.driver,
    config.databaseConfig.login,
    config.databaseConfig.password,
  )

  val parentStorage = MockParentStorage()
  val core =
    ParentCore(
      DatabaseStudentStorage(database),
      DatabaseGradeTable(database),
    )

  parentRun(botToken, parentStorage, core)
}
