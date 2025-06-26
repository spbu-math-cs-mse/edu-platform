package com.github.heheteam.commonlib.errors

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
