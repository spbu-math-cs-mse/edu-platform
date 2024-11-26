package com.github.heheteam.commonlib.mock

import com.github.heheteam.commonlib.*
import com.github.heheteam.commonlib.api.CoursesDistributor

const val PROBLEMS_PER_COURSE = 4

class MockCoursesDistributor : CoursesDistributor {
  val singleUserId = "0"
  private val students =
    mutableMapOf(
      "0" to Student("0"),
      "1" to Student("1"),
      "2" to Student("2"),
      "3" to Student("3"),
    )

  /*
  generates a course with dummy content with PROBLEMS_PER_COURSE problems
   */
  fun generateCourse(courseId: String, name: String): Course {
    val singleAssignmentId = courseId + ".a1"
    return Course(
      courseId,
      mutableListOf(),
      mutableListOf(),
      name,
      MockGradeTable(),
      assignments = mutableListOf(
        Assignment(
          singleAssignmentId,
          "sample assignment",
          problems = (1..PROBLEMS_PER_COURSE).map {
            Problem(
              id = singleAssignmentId + "p$it",
              number = it.toString(),
              description = "",
              assignmentId = singleAssignmentId,
              maxScore = 1,
            )
          }.toMutableList(),
          courseId = "0",
        ),
      ),
    )
  }

  private val courses =
    mutableMapOf(
      "0" to generateCourse("0", "Начала мат. анализа"),
      "1" to generateCourse("1", "Теория вероятностей"),
      "2" to generateCourse("2", "Линейная алгебра"),
      "3" to generateCourse("3", "ТФКП"),
    )

  private val data = mutableMapOf(
    singleUserId to mutableSetOf("0", "2"),
    "1" to mutableSetOf("1", "3"),
  )

  override fun addRecord(
    studentId: String,
    courseId: String,
  ) {
    if (!students.containsKey(studentId)) {
      students[studentId] = Student(studentId)
    }
    if (!data.containsKey(studentId)) {
      data[studentId] = mutableSetOf()
    }
    data[studentId]!!.add(courseId)
  }

  override fun getStudentCourses(studentId: String): List<Course> {
    if (!data.containsKey(studentId)) {
      return listOf()
    }
    return data[studentId]!!.map { courses[it]!! }
  }

  override fun getCourses(): List<Course> = courses.values.toList()

  override fun getTeacherCourses(teacherId: String): List<Course> = courses.values.filter { course -> course.teachers.map { it.id }.contains(teacherId) }
}
