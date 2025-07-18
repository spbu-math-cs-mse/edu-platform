package com.github.heheteam.commonlib.errors

import com.github.heheteam.commonlib.interfaces.TeacherId

data class ResolveError<T : Any>(
  val id: T,
  val objectClassName: String? = id::class.simpleName,
  override val causedBy: EduPlatformError? = null,
) : EduPlatformError {
  override val shortDescription = "Error: can not resolve $objectClassName with id=$id"
}

data class DeleteError<T>(
  val id: T,
  val rows: Int,
  override val causedBy: EduPlatformError? = null,
) : EduPlatformError {
  override val shortDescription: String = "Error: deleted $rows rows while deleting $id"
}

data class CreateError(
  val objectClassName: String,
  val message: String? = null,
  override val causedBy: EduPlatformError? = null,
) : EduPlatformError {
  override val shortDescription: String =
    "Error: can not create $objectClassName. " + if (message != null) "Reason: $message" else ""
}

data class BindError<T, U>(
  val id1: T,
  val id2: U,
  override val causedBy: EduPlatformError? = null,
) : EduPlatformError {
  override val shortDescription: String = "Error: can't bind $id1 to $id2"
}

sealed interface SubmissionResolveError : EduPlatformError

class TeacherDoesNotExist(val id: TeacherId, override val causedBy: EduPlatformError? = null) :
  SubmissionResolveError {
  override val shortDescription: String = "Учитель id=$id не существует"
}

data class TeacherResolveError(
  val message: String,
  override val causedBy: EduPlatformError? = null,
) : EduPlatformError {
  override val shortDescription: String = "Error identifying the teacher"
}

data class AdminIsNotWhitelistedError(
  val tgId: Long,
  override val causedBy: EduPlatformError? = null,
) : EduPlatformError {
  override val shortDescription: String =
    "Error resolving admin: Telegram id $tgId is not whitelisted"
}

data class DatabaseExceptionError(
  val exception: Throwable,
  override val causedBy: EduPlatformError? = null,
) : EduPlatformError {
  override val shortDescription: String = "Database exception: message = " + exception.message
  override val longDescription: String = exception.stackTraceToString()
}
