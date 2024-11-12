package com.github.heheteam.teacherbot

import dev.inmo.tgbotapi.types.UserId

class MockUserIdRegistry : UserIdRegistry {
    private val registry = mutableMapOf<UserId, String>()

    override fun getUserId(tgId: UserId): String? = registry[tgId]

    override fun setUserId(
        tgId: UserId,
        id: String,
    ) {
        registry[tgId] = id
    }
}
