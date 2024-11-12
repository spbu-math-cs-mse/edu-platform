package com.github.heheteam.studentbot

import dev.inmo.tgbotapi.types.UserId

class MockUserIdRegistry : UserIdRegistry {
    private val registry = mutableMapOf<UserId, String>()
    private var id = 1

    override fun getUserId(tgId: UserId): String = registry[tgId] ?: setUserId(tgId)

    override fun setUserId(
        tgId: UserId
    ): String {
        registry[tgId] = (id++).toString()
        return registry[tgId]!!
    }
}
