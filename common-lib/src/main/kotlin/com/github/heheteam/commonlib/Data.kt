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
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage
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

@Serializable
data class Teacher(
  val id: TeacherId,
  val name: String = "",
  val surname: String = "",
  val tgId: RawChatId,
)

@Serializable
data class Problem(
  val id: ProblemId,
  val serialNumber: Int,
  val number: String,
  val description: String,
  val maxScore: Grade,
  val assignmentId: AssignmentId,
  val deadline: LocalDateTime? = null,
)

data class ProblemDescription(
  val serialNumber: Int,
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

@Serializable
data class Solution(
  val id: SolutionId,
  val studentId: StudentId,
  val chatId: RawChatId,
  val messageId: MessageId,
  val problemId: ProblemId,
  val content: SolutionContent,
  val responsibleTeacherId: TeacherId?,
  val timestamp: LocalDateTime = java.time.LocalDateTime.now().toKotlinLocalDateTime(),
)

data class Course(val id: CourseId, val name: String)

data class Assignment(
  val id: AssignmentId,
  val serialNumber: Int,
  val description: String,
  val courseId: CourseId,
)

@Serializable data class SolutionAssessment(val grade: Grade, val comment: String = "")

@Serializable data class TelegramMessageInfo(val chatId: RawChatId, val messageId: MessageId)

fun AccessibleMessage.toTelegramMessageInfo(): TelegramMessageInfo =
  TelegramMessageInfo(this.chat.id.chatId, this.messageId)

data class MenuMessageInfo(val chatId: RawChatId, val messageId: MessageId? = null)

@Serializable
data class SolutionInputRequest(
  val studentId: StudentId,
  val problemId: ProblemId,
  val solutionContent: SolutionContent,
  val telegramMessageInfo: TelegramMessageInfo,
  val timestamp: LocalDateTime,
)
