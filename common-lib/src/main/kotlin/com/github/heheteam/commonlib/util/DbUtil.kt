package com.github.heheteam.commonlib.util

import com.github.heheteam.commonlib.errors.DatabaseExceptionError
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction

fun <T> catchingTransaction(
  database: Database,
  statement: Transaction.() -> T,
): Result<T, DatabaseExceptionError> {
  return runCatching { transaction(database, statement) }.mapError { DatabaseExceptionError(it) }
}
