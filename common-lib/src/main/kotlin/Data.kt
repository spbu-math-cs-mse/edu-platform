package com.github.heheteam.commonlib

import com.github.heheteam.commonlib.api.*
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import kotlinx.serialization.Serializable

typealias Grade = Int

data class Student(
  val id: StudentId,
  val name: String = "",
  val surname: String = "",
)

data class Parent(
  val id: ParentId,
  val name: String = "",
  val surname: String = "",
  val children: List<StudentId> = listOf(),
)

data class Teacher(
  val id: TeacherId,
  val name: String = "",
  val surname: String = "",
)

@Serializable
data class Problem(
  val id: ProblemId,
  val number: String,
  val description: String,
  val maxScore: Grade,
  val assignmentId: AssignmentId,
)

data class ProblemDescription(
  val number: String,
  val description: String = "",
  val maxScore: Grade = 1,
)

enum class SolutionType {
  TEXT,
  PHOTO,
  DOCUMENT,
  GROUP,
}

data class Solution(
  val id: SolutionId,
  val studentId: StudentId,
  val chatId: RawChatId,
  val messageId: MessageId,
  val problemId: ProblemId,
  val content: SolutionContent,
  val type: SolutionType,
  val timestamp: java.time.LocalDateTime = java.time.LocalDateTime.now(),
)

data class Course(
  val id: CourseId,
  val name: String,
)

data class Assignment(
  val id: AssignmentId,
  val description: String,
  val courseId: CourseId,
)

data class SolutionContent(
  val filesURL: List<String>? = null,
  val text: String? = null,
  val type: SolutionType? = null,
)

data class SolutionAssessment(
  val grade: Grade,
  val comment: String,
)
