package com.github.heheteam.commonlib.interfaces

import com.github.heheteam.commonlib.Parent
import com.github.heheteam.commonlib.ResolveError
import com.github.michaelbull.result.Result
import dev.inmo.tgbotapi.types.UserId

internal interface ParentStorage {
  fun createParent(): ParentId

  fun resolveParent(parentId: ParentId): Result<Parent, ResolveError<ParentId>>

  fun resolveByTgId(tgId: UserId): Result<Parent, ResolveError<UserId>>
}
