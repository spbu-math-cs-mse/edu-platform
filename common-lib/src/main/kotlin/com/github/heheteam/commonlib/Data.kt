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

@Serializable
enum class AttachmentKind {
  PHOTO,
  DOCUMENT,
}

@Serializable
data class SolutionAttachment(
  val kind: AttachmentKind,
  val downloadUrl: String,
  val uniqueString: String,
)

@Serializable
data class SolutionContent(
  val text: String = "",
  val attachments: List<SolutionAttachment> = listOf(),
)

data class Solution(
  val id: SolutionId,
  val studentId: StudentId,
  val chatId: RawChatId,
  val messageId: MessageId,
  val problemId: ProblemId,
  val attachments: SolutionContent,
  val timestamp: LocalDateTime = java.time.LocalDateTime.now().toKotlinLocalDateTime(),
)

data class Course(val id: CourseId, val name: String)

data class Assignment(val id: AssignmentId, val description: String, val courseId: CourseId)

data class SolutionAssessment(val grade: Grade, val comment: String)
