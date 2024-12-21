package com.github.heheteam.adminbot

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.Teacher

data class CourseStatistics(
  val studentsCount: Int,
  val teachersCount: Int,
  val assignmentsCount: Int,
  val totalProblems: Int,
  val totalMaxScore: Int,
  val totalSolutions: Int,
  val checkedSolutions: Int,
  val uncheckedSolutions: Int,
  val students: List<Student>,
  val teachers: List<Teacher>,
  val assignments: List<Assignment>,
) 
