package com.github.heheteam.adminbot

import com.github.heheteam.commonlib.*
import com.github.heheteam.commonlib.api.GradeTable
import dev.inmo.tgbotapi.types.Username
import java.time.LocalDateTime

class AdminCore(
  private val gradeTable: GradeTable,
  private val scheduledMessagesDistributor: ScheduledMessagesDistributor,
  private val coursesTable: MutableMap<String, Course>,
  private val studentsTable: MutableMap<Long, Student>,
  private val teachersTable: MutableMap<Long, Teacher>,
  private val adminsTable: List<Username>,
) : GradeTable, ScheduledMessagesDistributor {
  override fun addAssessment(student: Student, teacher: Teacher, solution: Solution, assessment: SolutionAssessment) =
    gradeTable.addAssessment(student, teacher, solution, assessment)

  override fun getStudentPerformance(studentId: Long): Map<Problem, Grade> =
    gradeTable.getStudentPerformance(studentId)

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
}
