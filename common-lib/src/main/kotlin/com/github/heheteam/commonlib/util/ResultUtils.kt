package com.github.heheteam.commonlib.util

import com.github.michaelbull.result.BindingScope
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.CoroutineBindingScope

fun <T> T.ok(): Result<T, Nothing> = Ok(this)

fun <ErrorT> BindingScope<ErrorT>.raiseError(err: ErrorT): Nothing {
  Err(err).bind()
}

suspend fun <ErrorT> CoroutineBindingScope<ErrorT>.raiseError(err: ErrorT): Nothing {
  Err(err).bind()
}

inline fun <V, E> Result<V, E>.ensureSuccess(block: (E) -> Nothing): V {
  return if (this.isOk) value else block(this.error)
}

fun <T> id(value: T) = value
