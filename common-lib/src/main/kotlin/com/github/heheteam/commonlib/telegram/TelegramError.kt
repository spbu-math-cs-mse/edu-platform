package com.github.heheteam.commonlib.telegram

import com.github.heheteam.commonlib.EduPlatformError

class TelegramError(val exception: Throwable) : EduPlatformError {
  override val causedBy: EduPlatformError?
    get() = null

  override val longDescription: String
    get() = exception.stackTraceToString()

  override val shortDescription: String
    get() =
      "Error while sending telegram message; exception ${exception.javaClass.name}" +
        " with message: \"${exception.message}\""
}
