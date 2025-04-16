package com.github.heheteam.commonlib

import com.github.heheteam.commonlib.interfaces.AssignmentId
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.ParentId
import com.github.heheteam.commonlib.interfaces.ProblemId
import com.github.heheteam.commonlib.interfaces.SolutionId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.TeacherId
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.serialization.Serializable

typealias Grade = Int

data class Student(
  val id: StudentId,
  val name: String = "",
  val surname: String = "",
  val tgId: RawChatId,
)

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
data class MediaAttachment(
  val kind: AttachmentKind,
  val downloadUrl: String,
  val uniqueString: String, // used do form a download file name
)

@Serializable
data class TextWithMediaAttachments(
  val text: String = "",
  val attachments: List<MediaAttachment> = listOf(),
)

@Serializable
data class Solution(
  val id: SolutionId,
  val studentId: StudentId,
  val chatId: RawChatId,
  val messageId: MessageId,
  val problemId: ProblemId,
  val content: TextWithMediaAttachments,
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

@Serializable
data class SolutionAssessment(
  val grade: Grade,
  val comment: TextWithMediaAttachments = TextWithMediaAttachments(),
)

@Serializable data class TelegramMessageInfo(val chatId: RawChatId, val messageId: MessageId)

fun AccessibleMessage.toTelegramMessageInfo(): TelegramMessageInfo =
  TelegramMessageInfo(this.chat.id.chatId, this.messageId)

data class MenuMessageInfo(val chatId: RawChatId, val messageId: MessageId? = null)

@Serializable
data class SolutionInputRequest(
  val studentId: StudentId,
  val problemId: ProblemId,
  val solutionContent: TextWithMediaAttachments,
  val telegramMessageInfo: TelegramMessageInfo,
  val timestamp: LocalDateTime,
)
