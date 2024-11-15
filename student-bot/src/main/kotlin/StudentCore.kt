package com.github.heheteam.studentbot

import com.github.heheteam.commonlib.CoursesDistributor
import com.github.heheteam.commonlib.SolutionDistributor
import com.github.heheteam.commonlib.UserIdRegistry

class StudentCore(
  private val solutionDistributor: SolutionDistributor,
  private val coursesDistributor: CoursesDistributor,
  private val userIdRegistry: UserIdRegistry,
) : UserIdRegistry by userIdRegistry,
  CoursesDistributor by coursesDistributor,
  SolutionDistributor by solutionDistributor
