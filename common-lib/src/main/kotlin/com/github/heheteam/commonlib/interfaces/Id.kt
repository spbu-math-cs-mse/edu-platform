package com.github.heheteam.commonlib.interfaces

import kotlinx.serialization.Serializable

@Serializable
data class CourseId(val long: Long) {
  override fun toString(): String = "$long"
}

@Serializable
data class ProblemId(val long: Long) {
  override fun toString(): String = "$long"
}

data class AdminId(val long: Long) {
  override fun toString(): String = "$long"
}

@Serializable
data class StudentId(val long: Long) {
  override fun toString(): String = "$long"
}

@Serializable
data class TeacherId(val long: Long) {
  override fun toString(): String = "$long"
}

data class ParentId(val long: Long) {
  override fun toString(): String = "$long"
}

@Serializable
data class SolutionId(val long: Long) {
  override fun toString(): String = "$long"
}

@Serializable
data class AssignmentId(val long: Long) {
  override fun toString(): String = "$long"
}

@Serializable
data class SpreadsheetId(val long: String) {
  override fun toString(): String = long
}

fun Long.toCourseId() = CourseId(this)

fun Long.toAssignmentId() = AssignmentId(this)

fun Long.toProblemId() = ProblemId(this)

fun Long.toSolutionId() = SolutionId(this)

fun Long.toStudentId() = StudentId(this)

fun Long.toTeacherId() = TeacherId(this)
