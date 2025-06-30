package com.github.heheteam.commonlib.errors

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapError

sealed interface FrontendError {
  val shouldBeIgnored: Boolean

  fun toMessageText(): String
}

class NumberedError(val number: Long, val error: EduPlatformError) :
  FrontendError, EduPlatformError by error {
  override val shouldBeIgnored: Boolean = false

  override fun toMessageText(): String =
    "Случилась ошибка! Не волнуйтесь, разработчики уже в пути решения этой проблемы.\n" +
      if (userDescription != null) "Ошибка №$number:\n$userDescription" else "Ошибка №$number"
}

class TelegramBotError(val error: EduPlatformError) : FrontendError, EduPlatformError by error {
  override val shouldBeIgnored: Boolean = true

  override fun toMessageText(): String =
    "Случилась ошибка! Не волнуйтесь, разработчики уже в пути решения этой проблемы.\n"
}

fun <T> Result<T, Throwable>.toTelegramError(): Result<T, TelegramBotError> =
  this.mapError { TelegramBotError(it.asEduPlatformError()) }
