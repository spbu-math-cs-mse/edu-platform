package com.github.heheteam.commonlib

import dev.inmo.tgbotapi.types.UserId

class MockUserIdRegistry : UserIdRegistry {
  private val registry = mutableMapOf<UserId, String>()
  private var id = 1

  override fun getUserId(tgId: UserId): String? = if (registry.contains(tgId)) registry[tgId] else null

  override fun setUserId(
    tgId: UserId,
  ) {
    registry[tgId] = (id++).toString()
  }

  override fun getRegistry(): Map<UserId, String> = registry.toMap()
}
