package com.github.heheteam.commonlib.api

data class ResolveError<T : Any>(
  val id: T,
  val objectClassName: String? = id::class.simpleName,
) {
  override fun toString(): String =
    "Error: can not resolve $objectClassName with id=$id"
}

data class DeleteError<T>(
  val id: T,
  val rows: Int,
) {
  override fun toString(): String =
    "Error: deleted $rows rows while deleting $id"
}

data class BindError<T, U>(
  val id1: T,
  val id2: U,
) {
  override fun toString(): String = "Error: can't bind $id1 to $id2"
}

sealed interface SolutionResolveError {
  fun toUserMessage(): String
}

class TeacherDoesNotExist(val id: TeacherId) : SolutionResolveError {
  override fun toUserMessage(): String = "Учитель id=$id не существует"
}
