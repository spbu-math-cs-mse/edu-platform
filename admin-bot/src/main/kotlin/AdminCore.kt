package com.github.heheteam.adminbot

import com.github.heheteam.commonlib.*
import com.github.heheteam.commonlib.api.*
import dev.inmo.tgbotapi.types.Username
import java.time.LocalDateTime

class AdminCore(
  private val gradeTable: GradeTable,
  private val scheduledMessagesDistributor: ScheduledMessagesDistributor,
  private val coursesTable: MutableMap<String, Course>,
  private val studentsTable: MutableMap<Long, Student>,
  private val teachersTable: MutableMap<Long, Teacher>,
  private val adminsTable: List<Username>,
  private val coursesDistributor: CoursesDistributor,
) : GradeTable, ScheduledMessagesDistributor {
  override fun addAssessment(
    teacherId: TeacherId,
    solutionId: SolutionId,
    assessment: SolutionAssessment,
  ) =
    gradeTable.addAssessment(teacherId, solutionId, assessment)

  override fun getStudentPerformance(
    studentId: StudentId,
    solutionDistributor: SolutionDistributor,
  ): Map<ProblemId, Grade> =
    gradeTable.getStudentPerformance(studentId, solutionDistributor)

  override fun addMessage(message: ScheduledMessage) =
    scheduledMessagesDistributor.addMessage(message)

  override fun getMessagesUpToDate(date: LocalDateTime): List<ScheduledMessage> =
    scheduledMessagesDistributor.getMessagesUpToDate(date)

  override fun markMessagesUpToDateAsSent(date: LocalDateTime) =
    scheduledMessagesDistributor.markMessagesUpToDateAsSent(date)

  fun courseExists(courseName: String) = coursesTable.containsKey(courseName)

  fun addCourse(courseName: String, course: Course) {
    coursesTable[courseName] = course
  }

  fun getCourse(courseName: String) = coursesTable[courseName]

  fun getCourses() = coursesTable.toMap()

  fun studentExists(id: Long) = studentsTable.containsKey(id)

  fun teacherExists(id: Long) = teachersTable.containsKey(id)

  fun isAdmin(username: Username) = adminsTable.contains(username)
  fun studiesIn(id: Long, course: Course): Boolean {
    return false
  }

  fun registerForCourse(studentId: StudentId, courseId: CourseId) {
    coursesDistributor.addRecord(studentId, courseId)
  }

  fun removeTeacher(teacherId: TeacherId, courseId: CourseId): Boolean {
    return false
  }

  fun removeStudent(studentId: StudentId, courseId: CourseId): Boolean {
    TODO()
  }
}
