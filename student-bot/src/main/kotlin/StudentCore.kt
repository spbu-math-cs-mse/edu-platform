package com.github.heheteam.studentbot

import GradeTable
import com.github.heheteam.commonlib.CoursesDistributor

class StudentCore(
  private val coursesDistributor: CoursesDistributor,
  private val gradeTable: GradeTable,
) : CoursesDistributor by coursesDistributor, GradeTable by gradeTable
