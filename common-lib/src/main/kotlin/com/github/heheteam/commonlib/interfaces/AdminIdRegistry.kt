package com.github.heheteam.commonlib.interfaces

import com.github.heheteam.commonlib.ResolveError
import com.github.michaelbull.result.Result
import dev.inmo.tgbotapi.types.UserId

internal interface AdminIdRegistry {
  fun getUserId(tgId: UserId): Result<AdminId, ResolveError<UserId>>
}
