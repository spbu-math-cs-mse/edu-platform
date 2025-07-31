package com.github.heheteam.commonlib.errors

import com.github.heheteam.commonlib.telegram.AdminBotTelegramController
import com.github.michaelbull.result.BindingScope
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.coroutines.CoroutineBindingScope
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.getError
import com.github.michaelbull.result.mapError
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.error
import dev.inmo.tgbotapi.types.RawChatId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class ErrorManagementService(val adminBotController: AdminBotTelegramController) {
  private companion object ErrorCounter {
    private var currentErrorNumber: Long = 1

    fun newErrorNumber(): Long = currentErrorNumber++
  }

  fun boundChat(chatId: RawChatId) {
    boundChat = chatId
  }

  private var boundChat: RawChatId? = null

  private fun EduPlatformError.toNumberedError(): NumberedError {
    return registerError(this)
  }

  fun registerError(error: EduPlatformError): NumberedError {
    val errorNumber = newErrorNumber()
    KSLog.error(
      "error $errorNumber\t" +
        "error description: ${error.longDescription}\n" +
        "stack trace:\n" +
        error.toStackedString()
    )
    return NumberedError(errorNumber, error)
  }

  private fun <U> Result<U, EduPlatformError>.toNumberedError(): Result<U, NumberedError> =
    this.mapError { it.toNumberedError() }

  fun <V> serviceBinding(block: BindingScope<EduPlatformError>.() -> V): Result<V, NumberedError> {
    val result = binding(block).toNumberedError()
    val error = result.getError()
    if (error != null) {
      val boundChat = boundChat
      if (boundChat != null) {
        runBlocking(Dispatchers.IO) { adminBotController.sendErrorInfo(boundChat, error) }
      }
    }
    return result
  }

  suspend fun <V> coroutineServiceBinding(
    block: suspend CoroutineBindingScope<EduPlatformError>.() -> V
  ): Result<V, NumberedError> = coroutineBinding(block).toNumberedError()
}
