package com.github.heheteam.teacherbot.states

import com.github.heheteam.commonlib.Grade
import com.github.heheteam.commonlib.SubmissionAssessment
import com.github.heheteam.commonlib.interfaces.SubmissionId
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent

sealed interface TeacherAction

data class GradingFromButton(val submissionId: SubmissionId, val grade: Grade) : TeacherAction

data class GradingFromReply(
  val submissionId: SubmissionId,
  val submissionAssessment: SubmissionAssessment,
) : TeacherAction

data class ConfirmSending(
  val submissionId: SubmissionId,
  val submissionAssessment: SubmissionAssessment,
  val messageToDeleteOnConfirm: ContentMessage<MessageContent>?,
) : TeacherAction

data class DeleteMessage(val message: AccessibleMessage?) : TeacherAction

data object UpdateMenuMessage : TeacherAction
