package com.github.heheteam.commonlib.errors

class NumberedError(val number: Long, val error: EduPlatformError) : EduPlatformError by error {
  fun toMessageText(): String =
    "Случилась ошибка! Не волнуйтесь, разработчики уже в пути решения этой проблемы.\n" +
      if (userDescription != null) "Ошибка №$number:\n$userDescription" else "Ошибка №$number"
}
