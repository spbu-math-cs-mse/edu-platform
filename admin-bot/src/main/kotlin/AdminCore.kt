package com.github.heheteam.adminbot

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.api.*
import java.time.LocalDateTime

class AdminCore(
  private val scheduledMessagesDistributor: ScheduledMessagesDistributor,
  private val coursesDistributor: CoursesDistributor,
  private val studentStorage: StudentStorage,
  private val teacherStorage: TeacherStorage,
) {
  fun addMessage(message: ScheduledMessage) = scheduledMessagesDistributor.addMessage(message)

  fun getMessagesUpToDate(date: LocalDateTime): List<ScheduledMessage> = scheduledMessagesDistributor.getMessagesUpToDate(date)

  fun markMessagesUpToDateAsSent(date: LocalDateTime) = scheduledMessagesDistributor.markMessagesUpToDateAsSent(date)

  fun courseExists(courseName: String): Boolean = getCourse(courseName) != null

  fun addCourse(courseName: String) {
    coursesDistributor.createCourse(courseName)
  }

  fun getCourse(courseName: String): Course? =
    coursesDistributor
      .getCourses()
      .asSequence()
      .map { coursesDistributor.resolveCourse(it) }
      .find { it?.description == courseName }

  fun getCourses(): Map<String, Course> =
    coursesDistributor
      .getCourses()
      .asSequence()
      .map { coursesDistributor.resolveCourse(it)!! }
      .groupBy { it.description }
      .mapValues { it.value.first() }

  fun studentExists(id: StudentId): Boolean = studentStorage.resolveStudent(id) != null

  fun teacherExists(id: TeacherId): Boolean = teacherStorage.resolveTeacher(id) != null

  fun studiesIn(
    id: StudentId,
    course: Course,
  ): Boolean = coursesDistributor.getStudentCourses(id).contains(course.id)

  fun registerStudentForCourse(
    studentId: StudentId,
    courseId: CourseId,
  ) {
    coursesDistributor.addStudentToCourse(studentId, courseId)
  }

  fun registerTeacherForCourse(
    teacherId: TeacherId,
    courseId: CourseId,
  ) {
    coursesDistributor.addTeacherToCourse(teacherId, courseId)
  }

  fun removeTeacher(
    teacherId: TeacherId,
    courseId: CourseId,
  ): Boolean {
    if (coursesDistributor.getTeacherCourses(teacherId).contains(courseId)) {
      coursesDistributor.removeTeacherFromCourse(teacherId, courseId)
      return true
    }
    return false
  }

  fun removeStudent(
    studentId: StudentId,
    courseId: CourseId,
  ): Boolean {
    if (coursesDistributor.getStudentCourses(studentId).contains(courseId)) {
      coursesDistributor.removeStudentFromCourse(studentId, courseId)
      return true
    }
    return false
  }
}
