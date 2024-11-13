package com.github.heheteam.teacherbot

import com.github.heheteam.commonlib.SolutionDistributor
import com.github.heheteam.commonlib.UserIdRegistry
import dev.inmo.tgbotapi.types.UserId

class TeacherCore(
    private val solutionDistributor: SolutionDistributor,
    private val userIdRegistry: UserIdRegistry,
) : UserIdRegistry by userIdRegistry,
    SolutionDistributor by solutionDistributor

var mockTgUsername: String = ""
