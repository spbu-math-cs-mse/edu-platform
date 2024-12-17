package com.github.heheteam.commonlib.mock

import com.github.heheteam.commonlib.api.*
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import dev.inmo.tgbotapi.types.UserId

class MockAdminIdRegistry(
  val usedId: Long,
) : AdminIdRegistry {
  override fun getUserId(tgId: UserId): Result<AdminId, ResolveError<UserId>> = Ok(AdminId(usedId))
}

class MockParentIdRegistry(
  val usedId: Long,
) : ParentIdRegistry {
  override fun getUserId(tgId: UserId): Result<ParentId, ResolveError<UserId>> = Ok(ParentId(usedId))
}
