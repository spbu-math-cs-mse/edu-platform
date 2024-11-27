package com.github.heheteam.adminbot

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.Teacher
import com.github.heheteam.commonlib.mock.MockGradeTable
import dev.inmo.tgbotapi.types.Username
import java.time.LocalDateTime

val mockGradeTable = MockGradeTable()

val mockCoursesTable: MutableMap<String, Course> =
  mutableMapOf(
    "Геома 1" to
      Course(
        1L,
        mutableListOf(Teacher(1L)),
        mutableListOf(Student(1L)),
        "какое-то описание",
        mockGradeTable,
      ),
  )

val mockStudentsTable: MutableMap<Long, Student> =
  mutableMapOf(
    1L to Student(1L),
    2L to Student(2L),
  )

val mockTeachersTable: MutableMap<Long, Teacher> =
  mutableMapOf(
    3L to Teacher(3L),
    4L to Teacher(4L),
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
