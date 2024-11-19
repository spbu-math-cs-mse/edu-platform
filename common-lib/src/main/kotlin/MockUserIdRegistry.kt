package com.github.heheteam.commonlib

import dev.inmo.tgbotapi.types.UserId

class MockUserIdRegistry(val usedId: String) : UserIdRegistry {
  override fun getUserId(tgId: UserId): String = usedId
}
