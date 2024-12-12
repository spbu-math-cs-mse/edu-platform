package com.github.heheteam.commonlib

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addPathSource
import kotlin.io.path.Path

data class Config(
  val databaseConfig: DatabaseConfig,
  val googleSheetsConfig: GoogleSheetsConfig,
)

fun loadConfig(): Config = ConfigLoaderBuilder
  .default()
  .addPathSource(Path("./../config.json"))
  .build()
  .loadConfigOrThrow<Config>()
