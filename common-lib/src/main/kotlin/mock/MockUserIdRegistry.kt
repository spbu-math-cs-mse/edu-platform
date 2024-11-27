package com.github.heheteam.commonlib.mock

import com.github.heheteam.commonlib.api.UserIdRegistry
import dev.inmo.tgbotapi.types.UserId

class MockUserIdRegistry(val usedId: Long) : UserIdRegistry {
  override fun getUserId(tgId: UserId): Long = usedId
}
