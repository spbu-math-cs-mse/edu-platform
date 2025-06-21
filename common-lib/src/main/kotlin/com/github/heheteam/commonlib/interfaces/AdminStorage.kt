package com.github.heheteam.commonlib.interfaces

import com.github.heheteam.commonlib.Admin
import com.github.heheteam.commonlib.EduPlatformError
import com.github.heheteam.commonlib.ResolveError
import com.github.michaelbull.result.Result
import dev.inmo.tgbotapi.types.UserId

internal interface AdminStorage {
  fun addTgIdToWhitelist(tgId: Long): Result<Unit, EduPlatformError>

  fun tgIdIsInWhitelist(tgId: Long): Boolean

  fun createAdmin(
    name: String = "defaultName",
    surname: String = "defaultSurname",
    tgId: Long = 0L,
  ): Result<AdminId, EduPlatformError>

  fun resolveAdmin(adminId: AdminId): Result<Admin, ResolveError<AdminId>>

  fun resolveByTgId(tgId: UserId): Result<Admin, ResolveError<UserId>>

  fun updateTgId(adminId: AdminId, newTgId: UserId): Result<Unit, ResolveError<AdminId>>

  fun getAdmins(): Result<List<Admin>, EduPlatformError>
}
