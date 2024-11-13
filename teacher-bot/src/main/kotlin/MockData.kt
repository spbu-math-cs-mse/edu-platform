package com.github.heheteam.samplebot

import Course
import Grade
import GradeTable
import Problem
import Series
import Solution
import SolutionAssessment
import SolutionContent
import Student
import Teacher
import kotlin.random.Random

val mockSolutions: MutableList<Solution> = mutableListOf()

var mockTgUsername: String = ""

val mockTeachers: MutableMap<String, Teacher> by lazy {
  mutableMapOf(
    mockTgUsername to Teacher("1"),
    "@somebody" to Teacher("2"),
  )
}

data class Quadruple<A, B, C, D>(
  val first: A,
  val second: B,
  val third: C,
  val fourth: D,
)

val mockCoursesTable = mutableMapOf(
  "0" to Course(
    "0",
    description = "Начала мат. анализа",
    series = mutableListOf(Series("0", "Серия 1", mutableListOf(Problem("0", 1, "", 10, "0"), Problem("1", 2, "", 5, "0")), "0")),
  ),
  "1" to Course(
    "1",
    description = "Теория вероятности",
    series = mutableListOf(Series("1", "Серия 1", mutableListOf(Problem("2", 1, "", 10, "1")), "1")),
  ),
  "2" to Course(
    "2",
    description = "Линейная алгебра",
    series = mutableListOf(
      Series(
        "2", "Серия 1",
        mutableListOf(Problem("3", 1, "", 10, "2"), Problem("4", 2, "", 5, "2"), Problem("5", 3, "", 5, "2")),
        "2",
      ),
    ),
  ),
  "3" to Course(
    "3",
    description = "Теория функции комплексной переменной",
    series = mutableListOf(Series("3", "Серия 1", mutableListOf(Problem("6", 1, "", 1, "3"), Problem("7", 2, "", 5, "3")), "3")),
  ),
)

fun Problem.getSeries(series: MutableList<Series> = mockCoursesTable.values.flatMap { it.series }.toMutableList()): Series? {
  return series.find { it.id == seriesId }
}

fun Series.getCourse(courses: MutableList<Course> = mockCoursesTable.values.toMutableList()): Course? {
  return courses.find { it.id == courseId }
}

val mockStudentsTable = mutableMapOf(
  "0" to Student("0", "Мария", "Кузнецова"),
  "1" to Student("1", "Иван", "Баландин"),
  "2" to Student("2"),
  "3" to Student("3"),
)

class MockGradeTable : GradeTable {
  private val data: MutableList<Quadruple<Student, Teacher, Solution, SolutionAssessment>> = mutableListOf()

  override fun addAssessment(student: Student, teacher: Teacher, solution: Solution, assessment: SolutionAssessment) {
    data.add(Quadruple(student, teacher, solution, assessment))
  }

  override fun getGradeMap(): Map<Student, Map<Problem, Grade>> {
    val result = mutableMapOf<Student, MutableMap<Problem, Grade>>()
    for (quadruple in data) {
      val student = quadruple.first
      val solution = quadruple.third
      val solutionAssessment = quadruple.fourth

      val problem = solution.problem
      val grade = solutionAssessment.grade

      val studentGrades = result.getOrPut(student) { mutableMapOf() }
      studentGrades[problem] = grade
    }
    return result
  }
}

var mockIncrementalSolutionId = 0
var wasMockGradeTableBuilt = false

// Student.id -> MutableList<Course.id>
val mockStudentsAndCourses = mutableMapOf(
  "0" to mutableListOf("1", "2"),
  "1" to mutableListOf("0", "3"),
)

fun GradeTable.buildMock(teacherId: String) {
  if (wasMockGradeTableBuilt) {
    return
  }
  wasMockGradeTableBuilt = true
  val coursesProcessed = mutableSetOf<String>()

  for (studentCourses in mockStudentsAndCourses) {
    val student = mockStudentsTable[studentCourses.key]!!
    val courses = studentCourses.value

    for (courseId in courses) {
      if (!coursesProcessed.contains(courseId)) {
        coursesProcessed.add(courseId)
        if (Random.nextBoolean()) {
          mockCoursesTable[courseId]!!.teachers.add(Teacher(teacherId))
        }
      }

      val course = mockCoursesTable[courseId]!!
      for (series in course.series) {
        for (problem in series.problems) {
          if (Random.nextBoolean()) {
            addAssessment(
              student,
              Teacher(if (course.teachers.map { it.id }.contains(teacherId)) teacherId else "0"),
              Solution((mockIncrementalSolutionId++).toString(), problem, SolutionContent(), SolutionType.TEXT),
              SolutionAssessment((0..problem.maxScore).random(), ""),
            )
          }
        }
      }
    }
  }
}
