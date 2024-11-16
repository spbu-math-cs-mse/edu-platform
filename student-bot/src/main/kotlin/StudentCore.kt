package com.github.heheteam.studentbot

import com.github.heheteam.commonlib.*

class StudentCore(
  val solutionDistributor: SolutionDistributor,
  val coursesDistributor: CoursesDistributor,
  val userIdRegistry: UserIdRegistry,
  val gradeTable: GradeTable,
) {
