package com.github.heheteam.commonlib.mock

import com.github.heheteam.commonlib.EduPlatformError
import com.github.heheteam.commonlib.Parent
import com.github.heheteam.commonlib.ResolveError
import com.github.heheteam.commonlib.interfaces.ParentId
import com.github.heheteam.commonlib.interfaces.ParentStorage
import com.github.heheteam.commonlib.util.ok
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import dev.inmo.tgbotapi.types.UserId

class MockParentStorage : ParentStorage {
  override fun createParent(): Result<ParentId, EduPlatformError> = ParentId(0).ok()

  override fun resolveParent(parentId: ParentId): Result<Parent, ResolveError<ParentId>> =
    Ok(Parent(parentId, children = listOf()))

  override fun resolveByTgId(tgId: UserId): Result<Parent, ResolveError<UserId>> =
    Ok(Parent(createParent().value))
}
