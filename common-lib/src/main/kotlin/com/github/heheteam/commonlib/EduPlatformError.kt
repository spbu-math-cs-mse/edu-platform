package com.github.heheteam.commonlib

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
