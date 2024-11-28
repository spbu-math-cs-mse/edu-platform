package com.github.heheteam.commonlib.mock

import com.github.heheteam.commonlib.*
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.CoursesDistributor
import com.github.heheteam.commonlib.api.StudentId

class MockCoursesDistributor : CoursesDistributor {
  val singleUserId = 0L
  private val students =
    mutableMapOf(
      0L to Student(0),
      1L to Student(1),
      2L to Student(2),
      3L to Student(3),
    )

  var lastFreeCourseId = 0L
  val courses = mutableMapOf<Long, Course>()

  private val studentsToCourseIds: MutableMap<Long, MutableSet<Long>> =
    mutableMapOf()

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

  override fun getStudentCourses(studentId: Long): List<CourseId> {
    if (!studentsToCourseIds.containsKey(studentId)) {
      return listOf()
    }
    return studentsToCourseIds[studentId]!!.map { it }
  }

  override fun getCourses(): List<CourseId> = courses.keys.toList()

  override fun getTeacherCourses(teacherId: Long): List<CourseId> =
    TODO()

  override fun resolveCourse(id: CourseId): Course? {
    return courses[id]
  }

  override fun createCourse(description: String): CourseId {
    val courseId = lastFreeCourseId++
    courses[courseId] = Course(courseId, description)
    return courseId
  }

  override fun getStudents(courseId: CourseId): List<StudentId> {
    return studentsToCourseIds
      .mapNotNull { if (it.value.contains(courseId)) it.key else null }
  }
}
