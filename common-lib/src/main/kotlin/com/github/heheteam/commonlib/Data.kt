package com.github.heheteam.commonlib

import com.github.heheteam.commonlib.api.AssignmentId
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.ParentId
import com.github.heheteam.commonlib.api.ProblemId
import com.github.heheteam.commonlib.api.SolutionId
import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.api.TeacherId
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.serialization.Serializable

typealias Grade = Int

data class Student(val id: StudentId, val name: String = "", val surname: String = "")

data class Parent(
  val id: ParentId,
  val name: String = "",
  val surname: String = "",
  val children: List<StudentId> = listOf(),
)

data class Teacher(val id: TeacherId, val name: String = "", val surname: String = "")

@Serializable
data class Problem(
  val id: ProblemId,
  val number: String,
  val description: String,
  val maxScore: Grade,
  val assignmentId: AssignmentId,
  val deadline: LocalDateTime? = null,
)

data class ProblemDescription(
  val number: String,
  val description: String = "",
  val maxScore: Grade = 1,
  val deadline: LocalDateTime? = null,
)

enum class SolutionType {
  TEXT,
  PHOTO,
  DOCUMENT,
  GROUP,
}

@Serializable
enum class TelegramMediaKind {
  PHOTO,
  DOCUMENT,
}

@Serializable
data class TelegramMedia(
  val kind: TelegramMediaKind,
  val downloadUrl: String,
  val uniqueString: String,
)

@Serializable
data class TelegramAttachment(val text: String, val media: List<TelegramMedia> = listOf())

data class Solution(
  val id: SolutionId,
  val studentId: StudentId,
  val chatId: RawChatId,
  val messageId: MessageId,
  val problemId: ProblemId,
  val content: SolutionContent,
  val attachments: TelegramAttachment,
  val timestamp: LocalDateTime = java.time.LocalDateTime.now().toKotlinLocalDateTime(),
)

data class Course(val id: CourseId, val name: String)

data class Assignment(val id: AssignmentId, val description: String, val courseId: CourseId)

data class SolutionContent(
  val filesURL: List<String>? = null,
  val text: String? = null,
  val type: SolutionType? = null,
)

data class SolutionAssessment(val grade: Grade, val comment: String)
