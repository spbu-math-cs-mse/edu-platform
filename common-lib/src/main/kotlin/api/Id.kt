package com.github.heheteam.commonlib.api

import com.github.heheteam.commonlib.Solution

data class CourseId(
  val id: Long,
)

data class ProblemId(
  val id: Long,
)

data class AdminId(
  val id: Long,
)

data class StudentId(
  val id: Long,
)

data class TeacherId(
  val id: Long,
)

data class ParentId(
  val id: Long,
)

data class SolutionId(
  val id: Long,
)

data class AssignmentId(
  val id: Long,
)

fun Long.toCourseId() = CourseId(this)
fun Long.toAssignmentId() = AssignmentId(this)
fun Long.toProblemId() = ProblemId(this)
fun Long.toSolutionId() = SolutionId(this)
fun Long.toStudentId() = StudentId(this)
