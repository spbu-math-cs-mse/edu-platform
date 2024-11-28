package com.github.heheteam.commonlib.mock

import com.github.heheteam.commonlib.*
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.CoursesDistributor
import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.api.TeacherId

class MockCoursesDistributor : CoursesDistributor {
  val singleUserId = 0L
  private val students =
    mutableMapOf(
      StudentId(0) to Student(StudentId(0)),
      StudentId(1L) to Student(StudentId(1)),
      StudentId(2L) to Student(StudentId(2)),
      StudentId(3L) to Student(StudentId(3)),
    )

  var lastFreeCourseId = 0L
  val courses = mutableMapOf<CourseId, Course>()

  private val studentsToCourseIds: MutableMap<StudentId, MutableSet<CourseId>> =
    mutableMapOf()

  override fun addRecord(
    studentId: StudentId,
    courseId: CourseId,
  ) {
    if (!students.containsKey(studentId)) {
      students[studentId] = Student(studentId)
    }
    if (!studentsToCourseIds.containsKey(studentId)) {
      studentsToCourseIds[studentId] = mutableSetOf()
    }
    studentsToCourseIds[studentId]!!.add(courseId)
  }

  override fun getStudentCourses(studentId: StudentId): List<CourseId> {
    if (!studentsToCourseIds.containsKey(studentId)) {
      return listOf()
    }
    return studentsToCourseIds[studentId]!!.map { it }
  }

  override fun getCourses(): List<CourseId> = courses.keys.toList()

  override fun getTeacherCourses(teacherId: TeacherId): List<CourseId> = TODO()

  override fun resolveCourse(id: CourseId): Course? = courses[id]

  override fun createCourse(description: String): CourseId {
    val courseId = CourseId(lastFreeCourseId++)
    courses[courseId] = Course(courseId, description)
    return courseId
  }

  override fun getStudents(courseId: CourseId): List<StudentId> =
    studentsToCourseIds
      .mapNotNull { if (it.value.contains(courseId)) it.key else null }
}
