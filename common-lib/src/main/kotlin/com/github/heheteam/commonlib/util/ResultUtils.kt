package com.github.heheteam.commonlib.util

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result

fun <T> T.ok(): Result<T, Nothing> = Ok(this)
