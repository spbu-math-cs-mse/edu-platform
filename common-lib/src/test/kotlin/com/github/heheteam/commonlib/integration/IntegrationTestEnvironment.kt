package com.github.heheteam.commonlib.integration

import com.github.heheteam.commonlib.TelegramMessageInfo
import com.github.heheteam.commonlib.api.ApiCollection
import com.github.heheteam.commonlib.api.ApiFabric
import com.github.heheteam.commonlib.api.TeacherResolverKind
import com.github.heheteam.commonlib.database.reset
import com.github.heheteam.commonlib.googlesheets.GoogleSheetsService
import com.github.heheteam.commonlib.loadConfig
import com.github.heheteam.commonlib.telegram.AdminBotTelegramController
import com.github.heheteam.commonlib.telegram.StudentBotTelegramController
import com.github.heheteam.commonlib.telegram.TeacherBotTelegramController
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.BeforeEach

open class IntegrationTestEnvironment {
  protected val config = loadConfig()
  protected val database =
    Database.connect(
      config.databaseConfig.url,
      config.databaseConfig.driver,
      config.databaseConfig.login,
      config.databaseConfig.password,
    )

  protected val googleSheetsService = mockk<GoogleSheetsService>(relaxed = true)
  protected val studentBotController = mockk<StudentBotTelegramController>(relaxed = true)
  protected val teacherBotController = mockk<TeacherBotTelegramController>(relaxed = true)
  protected val adminBotController = mockk<AdminBotTelegramController>(relaxed = true)

  protected val messageInfoNum = { num: Int ->
    TelegramMessageInfo(RawChatId(num.toLong()), MessageId(num.toLong()))
  }

  val testDispatcher = UnconfinedTestDispatcher()
  
  @OptIn(ExperimentalCoroutinesApi::class)
  protected fun createDefaultApis(): ApiCollection {
    return ApiFabric(
          database,
          config,
          googleSheetsService,
          studentBotController,
          teacherBotController,
          adminBotController,
      testDispatcher
    )
      .createApis(
        initDatabase = false,
        useRedis = false,
        teacherResolverKind = TeacherResolverKind.FIRST,
      )
  }

  @BeforeEach
  fun setup() {
    reset(database)
  }
}
