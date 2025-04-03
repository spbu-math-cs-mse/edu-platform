package com.github.heheteam.commonlib.interfaces

import kotlinx.serialization.Serializable

@Serializable
data class CourseId(val id: Long) {
  override fun toString(): String = "$id"
}

@Serializable
data class ProblemId(val id: Long) {
  override fun toString(): String = "$id"
}

data class AdminId(val id: Long) {
  override fun toString(): String = "$id"
}

@Serializable
data class StudentId(val id: Long) {
  override fun toString(): String = "$id"
}

@Serializable
data class TeacherId(val id: Long) {
  override fun toString(): String = "$id"
}

data class ParentId(val id: Long) {
  override fun toString(): String = "$id"
}

@Serializable
data class SolutionId(val id: Long) {
  override fun toString(): String = "$id"
}

@Serializable
data class AssignmentId(val id: Long) {
  override fun toString(): String = "$id"
}

@Serializable
data class SpreadsheetId(val id: String) {
  override fun toString(): String = id
}

fun Long.toCourseId() = CourseId(this)

fun Long.toAssignmentId() = AssignmentId(this)

fun Long.toProblemId() = ProblemId(this)

fun Long.toSolutionId() = SolutionId(this)

fun Long.toStudentId() = StudentId(this)

fun Long.toTeacherId() = TeacherId(this)
