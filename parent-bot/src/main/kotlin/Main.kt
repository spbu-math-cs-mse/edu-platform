package com.github.heheteam.parentbot

import com.github.heheteam.commonlib.database.DatabaseGradeTable
import com.github.heheteam.commonlib.database.DatabaseSolutionDistributor
import com.github.heheteam.commonlib.database.DatabaseStudentStorage
import com.github.heheteam.commonlib.loadConfig
import com.github.heheteam.commonlib.mock.*
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

  val userIdRegistry = MockParentIdRegistry(1)
  val parentStorage = MockParentStorage()
  val core =
    ParentCore(
      DatabaseStudentStorage(database),
      DatabaseGradeTable(database),
      DatabaseSolutionDistributor(database),
    )

  parentRun(botToken, userIdRegistry, parentStorage, core)
}
