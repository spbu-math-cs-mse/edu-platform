package com.github.heheteam.commonlib.errors

import com.github.michaelbull.result.Result
import dev.inmo.micro_utils.common.joinTo

interface EduPlatformError {
  val causedBy: EduPlatformError?
  val shortDescription: String
  val longDescription: String
    get() = shortDescription

  /** A simple, user-friendly description. It must be in Russian. */
  val userDescription: String?
    get() = null
}

data class NamedError(
  override val shortDescription: String,
  override val causedBy: EduPlatformError? = null,
) : EduPlatformError

data class OperationCancelledError(
  override val shortDescription: String = "Операция отменена.",
  override val causedBy: EduPlatformError? = null,
) : EduPlatformError

data class AggregateError(
  val summary: String,
  val causes: List<EduPlatformError>,
  override val causedBy: EduPlatformError? = null,
) : EduPlatformError {

  override val shortDescription: String = summary

  override val longDescription: String =
    "$summary: encountered issues:\n" +
      causes.joinToString("\n") { it.longDescription.prependIndent("  ") }
}

fun String.asNamedError(causedBy: EduPlatformError? = null) = NamedError(this, causedBy)

fun Throwable.asEduPlatformError(): EduPlatformError {
  val cause = this.cause
  return NamedError(
    shortDescription = this.message ?: this.javaClass.simpleName,
    causedBy = cause?.asEduPlatformError(),
  )
}

fun EduPlatformError.toStackedString(): String {
  val cause = this.causedBy
  val stackMessagePart =
    if (cause != null) {
      val causeStacktrace =
        generateSequence(cause) { it.causedBy }.map { it.shortDescription }.toList()
      "Stack trace:\n" + causeStacktrace.joinTo("\n") { "★ $it" }
    } else ""
  return "Error: ${this.shortDescription}\n" + stackMessagePart
}

typealias MaybeEduPlatformError = Result<Unit, EduPlatformError>
