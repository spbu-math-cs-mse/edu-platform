package com.github.heheteam.commonlib.config

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addFileSource

data class Config(
  val botConfig: BotConfig,
  val databaseConfig: DatabaseConfig,
  val googleSheetsConfig: GoogleSheetsConfig,
  val redisConfig: RedisConfig,
)

fun loadConfig(path: String): Config =
  ConfigLoaderBuilder.default().addFileSource(path).build().loadConfigOrThrow<Config>()
