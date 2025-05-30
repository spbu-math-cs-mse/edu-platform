package com.github.heheteam.commonlib

data class CourseStatistics(
  val studentsCount: Int,
  val teachersCount: Int,
  val assignmentsCount: Int,
  val totalProblems: Int,
  val totalMaxScore: Int,
  val totalSubmissions: Int,
  val checkedSubmissions: Int,
  val uncheckedSubmissions: Int,
  val students: List<Student>,
  val teachers: List<Teacher>,
  val assignments: List<Assignment>,
)
