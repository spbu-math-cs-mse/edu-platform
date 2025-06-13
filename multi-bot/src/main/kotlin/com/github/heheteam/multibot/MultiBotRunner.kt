package com.github.heheteam.multibot

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.heheteam.adminbot.AdminRunner
import com.github.heheteam.adminbot.formatters.CourseStatisticsFormatter
import com.github.heheteam.commonlib.api.ApiFabric
import com.github.heheteam.commonlib.api.TeacherResolverKind
import com.github.heheteam.commonlib.googlesheets.GoogleSheetsServiceDummy
import com.github.heheteam.commonlib.googlesheets.GoogleSheetsServiceImpl
import com.github.heheteam.commonlib.loadConfig
import com.github.heheteam.commonlib.telegram.AdminBotTelegramControllerImpl
import com.github.heheteam.commonlib.telegram.StudentBotTelegramControllerImpl
import com.github.heheteam.commonlib.telegram.TeacherBotTelegramControllerImpl
import com.github.heheteam.commonlib.toStackedString
import com.github.heheteam.parentbot.parentRun
import com.github.heheteam.studentbot.StudentRunner
import com.github.heheteam.teacherbot.StateRegister
import com.github.heheteam.teacherbot.TeacherRunner
import com.github.michaelbull.result.mapError
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.kslog.common.error
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import java.time.LocalDateTime
import korlibs.time.fromSeconds
import kotlin.time.Duration
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.toKotlinLocalDateTime
import org.jetbrains.exposed.sql.Database

private const val HEARTBEAT_DELAY_SECONDS = 5

class MultiBotRunner : CliktCommand() {
  private val studentBotToken: String by option().required().help("student bot token")
  private val teacherBotToken: String by option().required().help("teacher bot token")
  private val adminBotToken: String by option().required().help("admin bot token")
  private val parentBotToken: String by option().required().help("parent bot token")
  private val initDatabase: Boolean by option().flag("--noinit", default = true)
  private val enableSheets: Boolean by
    option("--enable-sheets").flag("--disable-sheets", default = true)

  private val studentBotUsername: String by option().required().help("student bot username")

  override fun run() {
    CourseStatisticsFormatter.studentBotUsername = studentBotUsername

    val config = loadConfig()
    val database =
      Database.connect(
        config.databaseConfig.url,
        config.databaseConfig.driver,
        config.databaseConfig.login,
        config.databaseConfig.password,
      )
    val googleSheetsService =
      if (enableSheets) {
        GoogleSheetsServiceImpl(config.googleSheetsConfig.serviceAccountKey)
      } else {
        GoogleSheetsServiceDummy()
      }
    val studentBot =
      telegramBot(studentBotToken) {
        logger = KSLog { level: LogLevel, tag: String?, message: Any, throwable: Throwable? ->
          println(defaultMessageFormatter(level, tag, message, throwable))
        }
      }

    val studentBotTelegramController = StudentBotTelegramControllerImpl(studentBot)

    val teacherBot =
      telegramBot(teacherBotToken) {
        logger = KSLog { level: LogLevel, tag: String?, message: Any, throwable: Throwable? ->
          println(defaultMessageFormatter(level, tag, message, throwable))
        }
      }

    val adminBot =
      telegramBot(adminBotToken) {
        logger = KSLog { level: LogLevel, tag: String?, message: Any, throwable: Throwable? ->
          println(defaultMessageFormatter(level, tag, message, throwable))
        }
      }

    val teacherBotTelegramController = TeacherBotTelegramControllerImpl(teacherBot)
    val adminBotTelegramController = AdminBotTelegramControllerImpl(adminBot)
    val apiFabric =
      ApiFabric(
        database,
        googleSheetsService,
        studentBotTelegramController,
        teacherBotTelegramController,
        adminBotTelegramController,
      )

    val apis = apiFabric.createApis(initDatabase, TeacherResolverKind.FIRST)

    runBlocking {
      launch { StudentRunner(studentBotToken, apis.studentApi).run() }
      launch {
        while (true) {
          val timestamp = LocalDateTime.now().toKotlinLocalDateTime()
          val result = apis.studentApi.checkAndSentMessages(timestamp)
          result.mapError {
            KSLog.error("Error while sending scheduled messages: ${it.toStackedString()}")
          }
          delay(Duration.fromSeconds(HEARTBEAT_DELAY_SECONDS))
        }
      }
      launch { TeacherRunner(teacherBotToken, StateRegister(apis.teacherApi)).run() }
      launch { AdminRunner(apis.adminApi).run(adminBotToken) }
      launch { parentRun(parentBotToken, apis.parentApi) }
    }
  }
}
