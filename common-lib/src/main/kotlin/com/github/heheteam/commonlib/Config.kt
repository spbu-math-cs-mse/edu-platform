package com.github.heheteam.commonlib

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addResourceSource

data class Config(
  val databaseConfig: DatabaseConfig,
  val googleSheetsConfig: GoogleSheetsConfig,
  val redisConfig: RedisConfig,
)

fun loadConfig(): Config =
  ConfigLoaderBuilder.default()
    .addResourceSource("/config.json")
    .build()
    .loadConfigOrThrow<Config>()
