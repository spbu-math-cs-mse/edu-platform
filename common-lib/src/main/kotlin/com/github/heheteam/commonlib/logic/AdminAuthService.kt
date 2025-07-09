package com.github.heheteam.commonlib.logic

import com.github.heheteam.commonlib.Admin
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.interfaces.AdminStorage
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import dev.inmo.tgbotapi.types.UserId

class AdminAuthService(private val adminStorage: AdminStorage) {

  fun loginByTgId(tgId: UserId): Result<Admin?, EduPlatformError> = adminStorage.resolveByTgId(tgId)

  fun tgIdIsInWhitelist(tgId: UserId): Result<Boolean, EduPlatformError> =
    adminStorage.tgIdIsInWhitelist(tgId.chatId.long)

  fun addTgIdToWhitelist(tgId: UserId): Result<Unit, EduPlatformError> =
    adminStorage.addTgIdToWhitelist(tgId.chatId.long)

  fun addTgIdsToWhitelist(tgIds: List<UserId>): Result<Unit, EduPlatformError> = binding {
    tgIds.forEach { tgId -> adminStorage.addTgIdToWhitelist(tgId.chatId.long).bind() }
  }

  fun createAdmin(name: String, surname: String, tgId: Long): Result<AdminId, EduPlatformError> =
    adminStorage.createAdmin(name, surname, tgId)
}
