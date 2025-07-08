package com.github.heheteam.commonlib.interfaces

import com.github.heheteam.commonlib.Admin
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.michaelbull.result.Result
import dev.inmo.tgbotapi.types.UserId

interface AdminStorage {
  fun addTgIdToWhitelist(tgId: Long): Result<Unit, EduPlatformError>

  fun tgIdIsInWhitelist(tgId: Long): Result<Boolean, EduPlatformError>

  fun createAdmin(
    name: String = "defaultName",
    surname: String = "defaultSurname",
    tgId: Long = 0L,
  ): Result<AdminId, EduPlatformError>

  fun resolveByTgId(tgId: UserId): Result<Admin?, EduPlatformError>

  fun getAdmins(): Result<List<Admin>, EduPlatformError>
}
