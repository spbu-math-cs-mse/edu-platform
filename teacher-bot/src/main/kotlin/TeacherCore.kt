package com.github.heheteam.teacherbot

import com.github.heheteam.commonlib.SolutionDistributor

class TeacherCore(
    private val solutionDistributor: SolutionDistributor,
    private val userIdRegistry: UserIdRegistry,
) : UserIdRegistry by userIdRegistry,
    SolutionDistributor by solutionDistributor

interface UserIdRegistry {
    fun getUserId(tgId: String): String?

    fun setUserId(
        tgId: String,
        id: String,
    )
}

var mockTgUsername: String = ""
