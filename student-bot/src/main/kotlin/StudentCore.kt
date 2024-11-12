package com.github.heheteam.studentbot

import com.github.heheteam.commonlib.CoursesDistributor
import com.github.heheteam.commonlib.SolutionDistributor

class StudentCore(
  private val solutionDistributor: SolutionDistributor,
  private val coursesDistributor: CoursesDistributor,
  private val userIdRegistry: UserIdRegistry,
) : UserIdRegistry by userIdRegistry,
  CoursesDistributor by coursesDistributor,
  SolutionDistributor by solutionDistributor

interface UserIdRegistry {
  fun getUserId(tgId: String): String

  fun setUserId(
    tgId: String,
    id: String,
  )
}
