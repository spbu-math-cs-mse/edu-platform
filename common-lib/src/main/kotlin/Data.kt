@file:Suppress("unused")

package com.github.heheteam.commonlib

import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.GradeTable
import com.github.heheteam.commonlib.api.ProblemId
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId

typealias Grade = Int

data class Student(
  val id: Long,
  val name: String = "",
  val surname: String = "",
)

data class Parent(
  val id: Long,
  val children: List<Student>,
)

data class Teacher(
  val id: Long,
)

data class Problem(
  val id: Long,
  val number: String,
  val description: String,
  val maxScore: Grade,
  val assignmentId: Long,
)

enum class SolutionType {
  TEXT,
  PHOTO,
  PHOTOS,
  DOCUMENT,
}

data class Solution(
  val id: Long,
  val studentId: Long,
  val chatId: RawChatId,
  val messageId: MessageId,
  val problemId: ProblemId,
  val content: SolutionContent,
  val type: SolutionType,
  val timestamp: java.time.LocalDateTime = java.time.LocalDateTime.now(),
)

class Course(
  val id: CourseId,
  val teachers: MutableList<Teacher> = mutableListOf(),
  val students: MutableList<Student> = mutableListOf(),
  var description: String,
  val gradeTable: GradeTable,
  val assignments: MutableList<Assignment> = mutableListOf(),
)

data class Assignment(
  val id: Long,
  val description: String,
  val problems: MutableList<Problem>,
  val courseId: Long,
)

data class SolutionContent(
  val fileIds: List<String>? = null,
  val text: String? = null,
)

data class SolutionAssessment(
  val grade: Grade,
  val comments: String,
)
