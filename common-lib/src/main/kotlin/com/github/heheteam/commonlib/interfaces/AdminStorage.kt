package com.github.heheteam.commonlib.interfaces

import com.github.heheteam.commonlib.Admin
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.heheteam.commonlib.errors.ResolveError
import com.github.michaelbull.result.Result
import dev.inmo.tgbotapi.types.UserId

interface AdminStorage {
  fun addTgIdToWhitelist(tgId: Long): Result<Unit, EduPlatformError>

  fun tgIdIsInWhitelist(tgId: Long): Boolean

  fun createAdmin(
    name: String = "defaultName",
    surname: String = "defaultSurname",
    tgId: Long = 0L,
  ): Result<AdminId, EduPlatformError>

  fun resolveAdmin(adminId: AdminId): Result<Admin, ResolveError<AdminId>>

  fun resolveByTgId(tgId: UserId): Result<Admin, ResolveError<UserId>>

  fun getAdmins(): Result<List<Admin>, EduPlatformError>
}
