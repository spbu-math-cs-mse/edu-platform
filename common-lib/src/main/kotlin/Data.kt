@file:Suppress("unused")

package com.github.heheteam.commonlib

import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId

data class Student(
  val id: String,
)

data class Parent(
  val id: String,
  val children: List<Student>,
)

data class Teacher(
  val id: String,
)

data class Problem(
  val id: String,
)

enum class SolutionType {
  TEXT,
  PHOTO,
  PHOTOS,
  DOCUMENT,
}

data class Solution(
  val id: String,
  val studentId: String,
  val chatId: RawChatId,
  val messageId: MessageId,
  val problem: Problem,
  val content: SolutionContent,
  val type: SolutionType,
  val timestamp: java.time.LocalDateTime = java.time.LocalDateTime.now(),
)

typealias Grade = Int

class Course(
  val id: String,
  val teachers: MutableList<Teacher>,
  val students: MutableList<Student>,
  var description: String,
  val gradeTable: GradeTable,
)

data class SolutionContent(
  val fileIds: List<String>? = null,
  val text: String? = null,
)

data class SolutionAssessment(
  val grade: Grade,
  val comments: String,
)
