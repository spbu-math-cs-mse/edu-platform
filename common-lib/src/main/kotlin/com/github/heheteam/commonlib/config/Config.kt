package com.github.heheteam.commonlib.config

import io.github.cdimascio.dotenv.Dotenv
import io.github.cdimascio.dotenv.dotenv
import java.io.File

fun loadConfig(path: String? = null): Config {
  val dotenv = dotenv {
    directory = path ?: "./"
    ignoreIfMalformed = true
    ignoreIfMissing = true
  }

  return Config(dotenv)
}

class Config(private val dotenv: Dotenv) {
  private fun env(name: String): String =
    dotenv.get(name)
      ?: error("Environment variable '$name' is not set. You can configure it with .env file.")

  private fun readSecret(name: String): String {
    return File(env(name)).readText().trim()
  }

  val botConfig by lazy {
    BotConfig(
      studentBotToken = readSecret("STUDENT_BOT_TOKEN_FILE"),
      teacherBotToken = readSecret("TEACHER_BOT_TOKEN_FILE"),
      adminBotToken = readSecret("ADMIN_BOT_TOKEN_FILE"),
      parentBotToken = readSecret("PARENT_BOT_TOKEN_FILE"),
      studentBotUsername = env("STUDENT_BOT_USERNAME"),
      adminIds = env("ADMIN_IDS").split(',').filter { it.isNotBlank() }.map { it.toLong() },
    )
  }

  val googleSheetsConfig =
    GoogleSheetsConfig(serviceAccountKeyPath = env("GOOGLE_SHEETS_SERVICE_ACCOUNT_KEY_FILE"))

  val databaseConfig =
    DatabaseConfig(
      url = System.getenv("DB_URL").ifEmpty { error("Environment variable DB_URL is not set.") },
      driver = env("DB_DRIVER"),
      login = readSecret("DB_LOGIN_FILE"),
      password = readSecret("DB_PASSWORD_FILE"),
    )
}
