package com.github.heheteam.commonlib.repository

import com.github.heheteam.commonlib.domain.RichParent
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.heheteam.commonlib.interfaces.ParentId
import com.github.michaelbull.result.Result
import dev.inmo.tgbotapi.types.RawChatId

interface ParentRepository {
  fun save(richParent: RichParent): Result<RichParent, EduPlatformError>

  fun findById(parentId: ParentId): Result<RichParent, EduPlatformError>

  fun findByTgId(tgId: RawChatId): Result<RichParent?, EduPlatformError>
}
