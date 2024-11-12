package com.github.heheteam.teacherbot

import com.github.heheteam.commonlib.SolutionDistributor
import dev.inmo.tgbotapi.types.UserId

class TeacherCore(
    private val solutionDistributor: SolutionDistributor,
    private val userIdRegistry: UserIdRegistry,
) : UserIdRegistry by userIdRegistry,
    SolutionDistributor by solutionDistributor

interface UserIdRegistry {
    fun getUserId(tgId: UserId): String?

    fun setUserId(
        tgId: UserId,
        id: String,
    )
}

var mockTgUsername: String = ""
