package com.github.heheteam.multibot

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.long
import com.github.heheteam.adminbot.adminRun
import com.github.heheteam.commonlib.api.ApiFabric
import com.github.heheteam.commonlib.api.TeacherResolverKind
import com.github.heheteam.commonlib.googlesheets.GoogleSheetsServiceImpl
import com.github.heheteam.commonlib.interfaces.toStudentId
import com.github.heheteam.commonlib.interfaces.toTeacherId
import com.github.heheteam.commonlib.loadConfig
import com.github.heheteam.commonlib.telegram.StudentBotTelegramControllerImpl
import com.github.heheteam.commonlib.telegram.TeacherBotTelegramControllerImpl
import com.github.heheteam.commonlib.util.DeveloperOptions
import com.github.heheteam.parentbot.parentRun
import com.github.heheteam.studentbot.studentRun
import com.github.heheteam.teacherbot.StateRegister
import com.github.heheteam.teacherbot.TeacherRunner
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database

class MultiBotRunner : CliktCommand() {
  private val studentBotToken: String by option().required().help("student bot token")
  private val teacherBotToken: String by option().required().help("teacher bot token")
  private val adminBotToken: String by option().required().help("admin bot token")
  private val parentBotToken: String by option().required().help("parent bot token")
  private val presetStudentId: Long? by option().long()
  private val presetTeacherId: Long? by option().long()
  private val useRedis: Boolean by option().boolean().default(false)
  private val initDatabase: Boolean by option().flag("--noinit", default = true)

  override fun run() {
    val config = loadConfig()
    val database =
      Database.connect(
        config.databaseConfig.url,
        config.databaseConfig.driver,
        config.databaseConfig.login,
        config.databaseConfig.password,
      )
    val googleSheetsService = GoogleSheetsServiceImpl(config.googleSheetsConfig.serviceAccountKey)
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

    val teacherBotTelegramController = TeacherBotTelegramControllerImpl(teacherBot)
    val apiFabric =
      ApiFabric(
        database,
        config,
        googleSheetsService,
        studentBotTelegramController,
        teacherBotTelegramController,
      )

    val apis = apiFabric.createApis(initDatabase, useRedis, TeacherResolverKind.FIRST)

    val developerOptions = run {
      val presetStudent = presetStudentId?.toStudentId()
      val presetTeacher = presetTeacherId?.toTeacherId()
      DeveloperOptions(presetStudent, presetTeacher)
    }
    runBlocking {
      launch { studentRun(studentBotToken, apis.studentApi, developerOptions) }
      launch {
        val stateRegister = StateRegister(apis.teacherApi)
        val teacherRunner = TeacherRunner(teacherBotToken, stateRegister, developerOptions)
        teacherRunner.execute(listOf())
      }
      launch { adminRun(adminBotToken, apis.adminApi) }
      launch { parentRun(parentBotToken, apis.parentApi) }
    }
  }
}
