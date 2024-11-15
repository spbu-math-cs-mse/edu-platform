package com.github.heheteam.studentbot

import com.github.heheteam.commonlib.CoursesDistributor
import com.github.heheteam.commonlib.GradeTable
import com.github.heheteam.commonlib.SolutionDistributor
import com.github.heheteam.commonlib.UserIdRegistry

class StudentCore(
  private val solutionDistributor: SolutionDistributor,
  private val coursesDistributor: CoursesDistributor,
  private val userIdRegistry: UserIdRegistry,
  private val gradeTable: GradeTable,
) : GradeTable by gradeTable,
  UserIdRegistry by userIdRegistry,
  CoursesDistributor by coursesDistributor,
  SolutionDistributor by solutionDistributor
