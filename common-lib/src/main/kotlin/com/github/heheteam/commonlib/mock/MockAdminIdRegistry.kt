package com.github.heheteam.commonlib.mock

import com.github.heheteam.commonlib.ResolveError
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.interfaces.AdminIdRegistry
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import dev.inmo.tgbotapi.types.UserId

class MockAdminIdRegistry(val usedId: Long) : AdminIdRegistry {
  override fun getUserId(tgId: UserId): Result<AdminId, ResolveError<UserId>> = Ok(AdminId(usedId))
}
