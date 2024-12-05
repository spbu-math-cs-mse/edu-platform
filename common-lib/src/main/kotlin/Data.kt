package com.github.heheteam.commonlib

import com.github.heheteam.commonlib.api.*
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId

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
  val children: List<StudentId>,
)

data class Teacher(
  val id: TeacherId,
  val name: String = "",
  val surname: String = "",
)

data class Problem(
  val id: ProblemId,
  val number: String,
  val description: String,
  val maxScore: Grade,
  val assignmentId: AssignmentId,
)

enum class SolutionType {
  TEXT,
  PHOTO,
  PHOTOS,
  DOCUMENT,
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
  val fileIds: List<String>? = null,
  val text: String? = null,
)

data class SolutionAssessment(
  val grade: Grade,
  val comment: String,
)
