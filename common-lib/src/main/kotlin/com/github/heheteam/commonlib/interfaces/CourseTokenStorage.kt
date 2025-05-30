package com.github.heheteam.commonlib.interfaces

import com.github.heheteam.commonlib.EduPlatformError
import com.github.michaelbull.result.Result

sealed class TokenError(override val causedBy: EduPlatformError? = null) : EduPlatformError {
  data object TokenNotFound : TokenError() {
    override val shortDescription: String = "Token not recognized/found"
  }

  data object TokenAlreadyUsed : TokenError() {
    override val shortDescription: String = "Token is already used"
  }

  fun toReadableString(): String =
    when (this) {
      is TokenNotFound -> "Такого токена не существует"

      is TokenAlreadyUsed -> "Этот токен уже был использован"
    }
}

interface CourseTokenStorage {
  fun createToken(courseId: CourseId): String

  fun regenerateToken(courseId: CourseId): String

  fun getCourseIdByToken(token: String): Result<CourseId, TokenError>

  fun useToken(token: String, studentId: StudentId): Result<Unit, TokenError>

  fun deleteToken(token: String): Result<Unit, TokenError>

  fun getTokenForCourse(courseId: CourseId): String?
}
