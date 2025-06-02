package com.github.heheteam.commonlib.logic

import com.github.heheteam.commonlib.EduPlatformError
import com.github.michaelbull.result.Result
import kotlinx.datetime.LocalDateTime

interface ScheduledMessageDeliveryService {
  fun checkAndSendMessages(timestamp: LocalDateTime): Result<Unit, EduPlatformError>
}
