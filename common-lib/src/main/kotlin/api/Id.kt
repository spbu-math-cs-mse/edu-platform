package com.github.heheteam.commonlib.api

import kotlinx.serialization.Serializable

data class CourseId(
    val id: Long,
)

@Serializable
data class ProblemId(
    val id: Long,
) {
    override fun toString(): String = "$id"
}

data class AdminId(
    val id: Long,
) {
    override fun toString(): String = "$id"
}

data class StudentId(
    val id: Long,
) {
    override fun toString(): String = "$id"
}

data class TeacherId(
    val id: Long,
) {
    override fun toString(): String = "$id"
}

data class ParentId(
    val id: Long,
) {
    override fun toString(): String = "$id"
}

data class SolutionId(
    val id: Long,
) {
    override fun toString(): String = "$id"
}

@Serializable
data class AssignmentId(
    val id: Long,
) {
    override fun toString(): String = "$id"
}

fun Long.toCourseId() = CourseId(this)

fun Long.toAssignmentId() = AssignmentId(this)

fun Long.toProblemId() = ProblemId(this)

fun Long.toSolutionId() = SolutionId(this)

fun Long.toStudentId() = StudentId(this)

fun Long.toTeacherId() = TeacherId(this)
