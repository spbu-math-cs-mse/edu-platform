package com.github.heheteam.commonlib.errors

import com.github.michaelbull.result.Result
import dev.inmo.micro_utils.common.joinTo
import dev.inmo.micro_utils.fsm.common.State
import kotlin.reflect.KClass

interface EduPlatformError {
  val causedBy: EduPlatformError?
  val shortDescription: String
  val longDescription: String
    get() = shortDescription

  val causedIn: KClass<*>?
    get() = null

  /** A simple, user-friendly description. It must be in Russian. */
  val userDescription: String?
    get() = null
}

data class UncaughtExceptionError(val exception: Throwable) : EduPlatformError {
  override val shortDescription: String = "uncaught exception"
  override val causedBy: EduPlatformError? = null
  override val longDescription: String
    get() = "Exception stack trace:\n" + exception.stackTraceToString()
}

data class StateError(
  override val shortDescription: String,
  override val causedIn: KClass<*>,
  override val causedBy: EduPlatformError? = null,
) : EduPlatformError

fun State.newStateError(shortDescription: String, causedBy: EduPlatformError? = null) =
  StateError(shortDescription, this::class, causedBy)

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

data class NamedError(
  override val shortDescription: String,
  override val causedIn: KClass<*>? = null,
  override val causedBy: EduPlatformError? = null,
) : EduPlatformError

fun String.asNamedError(service: KClass<*>, causedBy: EduPlatformError? = null) =
  NamedError(this, service, causedBy)

fun Throwable.asEduPlatformError(service: KClass<*>? = null): EduPlatformError {
  val cause = this.cause
  return NamedError(
    shortDescription = this.message ?: this.javaClass.simpleName,
    causedBy = cause?.asEduPlatformError(),
    causedIn = service,
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

typealias EduPlatformResult<T> = Result<T, EduPlatformError>
