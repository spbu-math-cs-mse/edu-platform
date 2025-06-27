package com.github.heheteam.commonlib.errors

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapError

class NumberedError(val number: Long, val error: EduPlatformError) : EduPlatformError by error {
  fun toMessageText(): String =
    "Случилась ошибка! Не волнуйтесь, разработчики уже в пути решения этой проблемы.\n" +
      if (userDescription != null) "Ошибка №$number:\n$userDescription" else "Ошибка №$number"
}

fun <T> Result<T, EduPlatformError>.toNumberedResult(): Result<T, NumberedError> =
  this.mapError { NumberedError(0, it) }

fun <T> Result<T, Throwable>.toNumberedResult(): Result<T, NumberedError> =
  this.mapError { NumberedError(0, it.asEduPlatformError()) }
