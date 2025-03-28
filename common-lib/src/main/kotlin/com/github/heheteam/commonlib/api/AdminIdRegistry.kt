package com.github.heheteam.commonlib.api

import com.github.heheteam.commonlib.ResolveError
import com.github.michaelbull.result.Result
import dev.inmo.tgbotapi.types.UserId

interface AdminIdRegistry {
  fun getUserId(tgId: UserId): Result<AdminId, ResolveError<UserId>>
}
