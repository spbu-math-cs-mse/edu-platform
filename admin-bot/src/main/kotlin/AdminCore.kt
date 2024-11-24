package com.github.heheteam.adminbot

import com.github.heheteam.commonlib.*
import dev.inmo.tgbotapi.types.Username
import java.time.LocalDateTime

class AdminCore(
  private val gradeTable: GradeTable,
  private val scheduledMessagesDistributor: ScheduledMessagesDistributor,
  private val coursesTable: MutableMap<String, Course>,
  private val studentsTable: MutableMap<String, Student>,
  private val teachersTable: MutableMap<String, Teacher>,
  private val adminsTable: List<Username>,
) : GradeTable, ScheduledMessagesDistributor
{
  fun studentExists(id: String) = studentsTable.containsKey(id)

  fun teacherExists(id: String) = teachersTable.containsKey(id)

  fun courseExists(courseTitle: String) = coursesTable.containsKey(courseTitle)

  fun addCourse(courseTitle: String, course: Course) {
    coursesTable[courseTitle] = course
  }

  fun getCourse(courseTitle: String) = coursesTable[courseTitle]

  fun getCourses() = coursesTable.toMap()

  fun isAdmin(username: Username) = adminsTable.contains(username)

  override fun addMessage(message: ScheduledMessage) =
    scheduledMessagesDistributor.addMessage(message)

  override fun getMessagesUpToDate(date: LocalDateTime): List<ScheduledMessage> =
    scheduledMessagesDistributor.getMessagesUpToDate(date)

  override fun markMessagesUpToDateAsSent(date: LocalDateTime) =
    scheduledMessagesDistributor.markMessagesUpToDateAsSent(date)

  override fun addAssessment(student: Student, teacher: Teacher, solution: Solution, assessment: SolutionAssessment) =
    gradeTable.addAssessment(student, teacher, solution, assessment)
  override fun getGradeMap(): Map<Student, Map<Problem, Grade>> =
    gradeTable.getGradeMap()
}
