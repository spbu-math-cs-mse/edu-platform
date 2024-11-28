package com.github.heheteam.adminbot

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.Teacher
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.mock.InMemoryGradeTable
import dev.inmo.tgbotapi.types.Username
import java.time.LocalDateTime

val mockGradeTable = InMemoryGradeTable()

val mockCoursesTable: MutableMap<String, Course> =
  mutableMapOf(
    "Геома 1" to
      Course(
        CourseId(1L),
        "какое-то описание",
      ),
  )

val mockStudentsTable: MutableMap<StudentId, Student> =
  mutableMapOf(
    StudentId(1L) to Student(StudentId(1L)),
    StudentId(2L) to Student(StudentId(2L)),
  )

val mockTeachersTable: MutableMap<TeacherId, Teacher> =
  mutableMapOf(
    TeacherId(3L) to Teacher(TeacherId(3L)),
    TeacherId(4L) to Teacher(TeacherId(4L)),
  )

val mockAdminsTable: List<Username> =
  listOf(
    Username("@schindleria_praematurus"),
  )

class InMemoryScheduledMessagesDistributor(
  private val messages: MutableMap<ScheduledMessage, Boolean> = mutableMapOf(),
) : ScheduledMessagesDistributor {
  override fun addMessage(message: ScheduledMessage) {
    messages[message] = false
  }

  override fun getMessagesUpToDate(date: LocalDateTime): List<ScheduledMessage> {
    val res = mutableListOf<ScheduledMessage>()
    for (message in messages) {
      if (!message.value && date.isAfter(message.key.date)) {
        res.addLast(message.key)
      }
    }
    return res.toList()
  }

  override fun markMessagesUpToDateAsSent(date: LocalDateTime) {
    for (message in messages) {
      if (!message.value && date.isAfter(message.key.date)) {
        messages[message.key] = true
      }
    }
  }
}
