@file:Suppress("unused")

package com.github.heheteam.commonlib

import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId

typealias Grade = Int

data class Student(
  val id: String,
  val name: String = "",
  val surname: String = "",
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
  val number: String,
  val description: String,
  val maxScore: Grade,
  val seriesId: String,
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

class Course(
  val id: String,
  val teachers: MutableList<Teacher> = mutableListOf(),
  val students: MutableList<Student> = mutableListOf(),
  var description: String,
  val series: MutableList<Series> = mutableListOf(),
)

data class Series(
  val id: String,
  val description: String,
  val problems: MutableList<Problem>,
  val courseId: String,
)

data class SolutionContent(
  val fileIds: List<String>? = null,
  val text: String? = null,
)

data class SolutionAssessment(
  val grade: Grade,
  val comments: String,
)
