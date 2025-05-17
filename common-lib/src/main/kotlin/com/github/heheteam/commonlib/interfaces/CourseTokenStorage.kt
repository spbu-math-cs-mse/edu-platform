package com.github.heheteam.commonlib.interfaces

import com.github.michaelbull.result.Result

sealed class TokenError {
  data object TokenNotFound : TokenError()

  data object TokenAlreadyUsed : TokenError()
}

interface CourseTokenStorage {
  fun createToken(courseId: CourseId): String

  fun getCourseIdByToken(token: String): Result<CourseId, TokenError>

  fun useToken(token: String, studentId: StudentId): Result<Unit, TokenError>

  fun deleteToken(token: String): Result<Unit, TokenError>
}
