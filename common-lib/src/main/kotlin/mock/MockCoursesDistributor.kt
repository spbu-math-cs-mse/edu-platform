package com.github.heheteam.commonlib.mock

import com.github.heheteam.commonlib.*
import com.github.heheteam.commonlib.api.CoursesDistributor

const val PROBLEMS_PER_COURSE = 4

class MockCoursesDistributor : CoursesDistributor {
  val singleUserId = 0L
  private val students =
    mutableMapOf(
      0L to Student(0),
      1L to Student(1),
      2L to Student(2),
      3L to Student(3),
    )

  /*
  generates a course with dummy content with PROBLEMS_PER_COURSE problems
   */
  fun generateCourse(courseId: Long, name: String): Course {
    val singleAssignmentId = courseId * 10 + 1
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
              id = singleAssignmentId * 100 + it,
              number = it.toString(),
              description = "",
              assignmentId = singleAssignmentId,
              maxScore = 1,
            )
          }.toMutableList(),
          courseId = 0,
        ),
      ),
    )
  }

  private val courses =
    mutableMapOf(
      0L to generateCourse(0L, "Начала мат. анализа"),
      1L to generateCourse(1L, "Теория вероятностей"),
      2L to generateCourse(2L, "Линейная алгебра"),
      3L to generateCourse(3L, "ТФКП"),
    )

  private val studentsToCourseIds = mutableMapOf(
    singleUserId to mutableSetOf(0L, 2L),
    1L to mutableSetOf(1L, 3L),
  )

  override fun addRecord(
    studentId: Long,
    courseId: Long,
  ) {
    if (!students.containsKey(studentId)) {
      students[studentId] = Student(studentId)
    }
    if (!studentsToCourseIds.containsKey(studentId)) {
      studentsToCourseIds[studentId] = mutableSetOf()
    }
    studentsToCourseIds[studentId]!!.add(courseId)
  }

  override fun getStudentCourses(studentId: Long): List<Course> {
    if (!studentsToCourseIds.containsKey(studentId)) {
      return listOf()
    }
    return studentsToCourseIds[studentId]!!.map { courses[it]!! }
  }

  override fun getCourses(): List<Course> = courses.values.toList()

  override fun getTeacherCourses(teacherId: Long): List<Course> =
    courses.values.filter { course ->
      course.teachers.map { it.id }.contains(teacherId)
    }
}
