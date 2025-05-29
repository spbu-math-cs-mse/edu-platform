package com.github.heheteam.commonlib

import dev.inmo.micro_utils.common.joinTo

interface EduPlatformError {
  val causedBy: EduPlatformError?
  val shortDescription: String
  val longDescription: String
    get() = shortDescription
}

data class NamedError(
  override val shortDescription: String,
  override val causedBy: EduPlatformError? = null,
) : EduPlatformError

fun String.asNamedError(causedBy: EduPlatformError? = null) = NamedError(this, causedBy)

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
