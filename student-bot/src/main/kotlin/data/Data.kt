package com.github.heheteam.samplebot.data

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

interface CoursesDistributor {
  fun addRecord(studentId: String, courseId: String)
  fun getCourses(studentId: String): String
  fun getListOfCourses(studentId: String): List<Course>
  fun getAvailableCourses(studentId: String): MutableList<Pair<Course, Boolean>>
}

data class Course(val id: String, val description: String, val series: MutableList<Series> = mutableListOf())

fun Problem.getSeries(series: MutableList<Series>): Series? {
  return series.find { it.id == seriesId }
}

fun Series.getCourse(courses: MutableList<Course>): Course? {
  return courses.find { it.id == courseId }
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
    "Начала мат. анализа",
    mutableListOf(Series("0", "Серия 1", mutableListOf(Problem("0", 1, "", 10, "0"), Problem("1", 2, "", 5, "0")), "0")),
  ),
  "1" to Course(
    "1",
    "Теория вероятности",
    mutableListOf(Series("1", "Серия 1", mutableListOf(Problem("2", 1, "", 10, "1")), "1")),
  ),
  "2" to Course(
    "2",
    "Линейная алгебра",
    mutableListOf(
      Series(
        "2", "Серия 1",
        mutableListOf(Problem("3", 1, "", 10, "2"), Problem("4", 2, "", 5, "2"), Problem("5", 3, "", 5, "2")),
        "2",
      ),
    ),
  ),
  "3" to Course(
    "3",
    "Теория функции комплексной переменной",
    mutableListOf(Series("3", "Серия 1", mutableListOf(Problem("6", 1, "", 1, "3"), Problem("7", 2, "", 5, "3")), "3")),
  ),
)

val mockStudentsTable = mutableMapOf(
  "0" to Student("0"),
  "1" to Student("1"),
  "2" to Student("2"),
  "3" to Student("3"),
)

val mockAvailableCoursesTable = mutableMapOf(
  "0" to mutableMapOf(
    "0" to Pair(mockCoursesTable["0"]!!, true),
    "1" to Pair(mockCoursesTable["1"]!!, false),
    "2" to Pair(mockCoursesTable["2"]!!, false),
    "3" to Pair(mockCoursesTable["3"]!!, true),
  ),
  "1" to mutableMapOf(
    "0" to Pair(mockCoursesTable["0"]!!, false),
    "1" to Pair(mockCoursesTable["1"]!!, true),
    "2" to Pair(mockCoursesTable["2"]!!, true),
    "3" to Pair(mockCoursesTable["3"]!!, false),
  ),
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

fun GradeTable.buildMock(studentId: String, coursesDistributor: CoursesDistributor) {
  val courses = coursesDistributor.getListOfCourses(studentId)
  for (course in courses) {
    for (series in course.series) {
      for (problem in series.problems) {
        if (Random.nextBoolean()) {
          addAssessment(
            Student(studentId),
            Teacher("0"),
            Solution((mockIncrementalSolutionId++).toString(), problem, SolutionContent(), SolutionType.TEXT),
            SolutionAssessment((0..problem.maxScore).random(), ""),
          )
        }
      }
    }
  }
}

class MockCoursesDistributor : CoursesDistributor {
  private val students = mockStudentsTable
  private val courses = mockCoursesTable
  private val available = mockAvailableCoursesTable

  private val data = mutableMapOf(
    "0" to mutableListOf("1", "2"),
    "1" to mutableListOf("0", "3"),
  )

  override fun addRecord(studentId: String, courseId: String) {
    if (!students.containsKey(studentId)) {
      students[studentId] = Student(studentId)
      data[studentId] = mutableListOf()
    }
    buildCoursesForStudent(studentId)
    available[studentId]!![courseId] = Pair(mockCoursesTable[courseId]!!, false)
    data[studentId]!!.add(courseId)
  }

  override fun getCourses(studentId: String): String {
    if (!data.containsKey(studentId)) {
      return "Вы не записаны ни на один курс!"
    }
    return data[studentId]!!.map { courses[it]!!.description }.joinToString(separator = "\n") { "- $it" }
  }

  override fun getListOfCourses(studentId: String): List<Course> {
    if (!data.containsKey(studentId)) {
      return listOf()
    }
    return data[studentId]!!.map { courses[it]!! }
  }

  override fun getAvailableCourses(studentId: String): MutableList<Pair<Course, Boolean>> {
    buildCoursesForStudent(studentId)
    return available[studentId]!!.values.toMutableList()
  }

  private fun buildCoursesForStudent(studentId: String) {
    if (!available.containsKey(studentId)) {
      available[studentId] = mutableMapOf<String, Pair<Course, Boolean>>().apply {
        mockCoursesTable.forEach { (key, course) ->
          this[key] = Pair(course, true)
        }
      }
    }
  }
}
