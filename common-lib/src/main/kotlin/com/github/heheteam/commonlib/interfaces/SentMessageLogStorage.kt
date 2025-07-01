package com.github.heheteam.commonlib.interfaces

import com.github.heheteam.commonlib.SentMessageLog
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.michaelbull.result.Result

interface SentMessageLogStorage {
  fun logSentMessage(log: SentMessageLog): Result<Unit, EduPlatformError>

  fun getSentMessageLogs(
    scheduledMessageId: ScheduledMessageId
  ): Result<List<SentMessageLog>, EduPlatformError>
}
