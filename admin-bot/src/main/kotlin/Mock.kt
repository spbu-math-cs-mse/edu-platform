package com.github.heheteam.adminbot

import Course
import MockGradeTable
import Student
import Teacher
import dev.inmo.tgbotapi.types.Username
import java.time.LocalDateTime

val mockGradeTable = MockGradeTable()

val mockCoursesTable: MutableMap<String, Course> = mutableMapOf(
  "Геома 1" to Course(
    mutableListOf(Teacher("1")), mutableListOf(Student("1")),
    "какое-то описание", mockGradeTable,
  ),
)

val mockStudentsTable: MutableMap<String, Student> = mutableMapOf(
  "1" to Student("1"),
  "2" to Student("2"),
)

val mockTeachersTable: MutableMap<String, Teacher> = mutableMapOf(
  "3" to Teacher("3"),
  "4" to Teacher("4"),
)

val mockAdminsTable: List<Username> = listOf(
  Username("@schindleria_praematurus"),
)

class MockScheduledMessagesDistributor(
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
