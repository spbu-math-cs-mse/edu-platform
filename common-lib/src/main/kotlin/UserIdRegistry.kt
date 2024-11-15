package com.github.heheteam.commonlib

import dev.inmo.tgbotapi.types.UserId

interface UserIdRegistry {
  fun getUserId(tgId: UserId): String?

  fun setUserId(tgId: UserId)

  fun getRegistry(): Map<UserId, String>
}
