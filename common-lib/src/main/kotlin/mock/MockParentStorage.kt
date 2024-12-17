package com.github.heheteam.commonlib.mock

import com.github.heheteam.commonlib.Parent
import com.github.heheteam.commonlib.api.ParentId
import com.github.heheteam.commonlib.api.ParentStorage
import com.github.heheteam.commonlib.api.ResolveError
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import dev.inmo.tgbotapi.types.UserId

class MockParentStorage : ParentStorage {
  override fun createParent(): ParentId = ParentId(0)

  override fun resolveParent(parentId: ParentId): Parent =
    Parent(parentId, children = listOf())

  override fun resolveByTgId(tgId: UserId): Result<ParentId, ResolveError<UserId>> =
    Ok(createParent())
}
