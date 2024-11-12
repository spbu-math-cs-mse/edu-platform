@file:Suppress("unused")

package com.github.heheteam.commonlib

data class Student(
  val id: String,
)

data class Parent(
  val id: String,
  val children: List<Student>,
)

data class Teacher(
  val id: String,
)

data class Problem(
  val id: String,
)

enum class SolutionType {
  TEXT,
  PHOTO,
  PHOTOS,
  DOCUMENT,
}

data class Solution(
  val id: String,
  val problem: Problem,
  val content: SolutionContent,
  val type: SolutionType,
)
typealias Grade = Int

class Course(
  val id: String,
  val teachers: MutableList<Teacher>,
  val students: MutableList<Student>,
  var description: String,
  val gradeTable: GradeTable,
)

data class SolutionContent(
  val fileIds: List<String>? = null,
  val text: String? = null,
)

data class SolutionAssessment(
  val grade: Grade,
  val comments: String,
)

interface GradeTable {
  fun addAssessment(
    student: Student,
    teacher: Teacher,
    solution: Solution,
    assessment: SolutionAssessment,
  )

  fun getGradeMap(): Map<Student, Map<Problem, Grade>>
}

interface SolutionDistributor {
  fun inputSolution(
    studentId: String,
    solutionContent: SolutionContent,
  ): Solution

  fun querySolution(teacherId: String): Solution?

  fun assessSolution(
    solution: Solution,
    teacherId: String,
    assessment: SolutionAssessment,
    gradeTable: GradeTable,
  )
}

interface CoursesDistributor {
  fun addRecord(
    studentId: String,
    courseId: String,
  )

  fun getCourses(studentId: String): String

  fun getAvailableCourses(studentId: String): MutableList<Pair<Course, Boolean>>
}
