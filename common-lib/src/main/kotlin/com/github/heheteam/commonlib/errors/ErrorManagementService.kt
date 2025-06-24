package com.github.heheteam.commonlib.errors

import com.github.michaelbull.result.BindingScope
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.coroutines.CoroutineBindingScope
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.mapError
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.error

class ErrorManagementService {
  private companion object ErrorCounter {
    private var currentErrorNumber: Long = 1

    fun newErrorNumber(): Long = currentErrorNumber++
  }

  private fun EduPlatformError.toNumberedError(): NumberedError {
    val errorNumber = newErrorNumber()
    KSLog.error(
      "error $errorNumber\t" +
        "error description: ${this.longDescription}\n" +
        "stack trace:\n" +
        this.toStackedString()
    )
    return NumberedError(errorNumber, this)
  }

  private fun <U> Result<U, EduPlatformError>.toNumberedError(): Result<U, NumberedError> =
    this.mapError { it.toNumberedError() }

  fun <V> serviceBinding(block: BindingScope<EduPlatformError>.() -> V): Result<V, NumberedError> =
    binding(block).toNumberedError()

  suspend fun <V> coroutineServiceBinding(
    block: suspend CoroutineBindingScope<EduPlatformError>.() -> V
  ): Result<V, NumberedError> = coroutineBinding(block).toNumberedError()
}
