package com.github.heheteam.teacherbot.states

import com.github.heheteam.commonlib.Grade
import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.interfaces.SolutionId
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent

sealed interface TeacherAction

data class GradingFromButton(val solutionId: SolutionId, val grade: Grade) : TeacherAction

data class GradingFromReply(
  val solutionId: SolutionId,
  val solutionAssessment: SolutionAssessment,
) : TeacherAction

data class ConfirmSending(
  val solutionId: SolutionId,
  val solutionAssessment: SolutionAssessment,
  val messageToDeleteOnConfirm: ContentMessage<MessageContent>?,
) : TeacherAction

data class DeleteMessage(val message: AccessibleMessage?) : TeacherAction

data object UpdateMenuMessage : TeacherAction
