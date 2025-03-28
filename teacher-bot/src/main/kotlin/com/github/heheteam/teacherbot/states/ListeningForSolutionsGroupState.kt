package com.github.heheteam.teacherbot.states

import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.CoursesDistributor
import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.logic.AcademicWorkflowService
import com.github.heheteam.commonlib.util.waitDataCallbackQueryWithUser
import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.mapError
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.Chat
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.toChatId
import java.time.LocalDateTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.datetime.toKotlinLocalDateTime

class ListeningForSolutionsGroupState(override val context: Chat, val courseId: CourseId) : State {
  suspend fun execute(
    bot: BehaviourContext,
    academicWorkflowService: AcademicWorkflowService,
    coursesDistributor: CoursesDistributor,
  ): State {
    with(bot) {
      coursesDistributor.setCourseGroup(courseId, context.id.chatId)
      while (true) {
        merge(
            waitTextMessageWithUser(context.id.toChatId()).map { commonMessage ->
              val result = tryParseGradingReply(commonMessage, academicWorkflowService)
              result.mapError { errorMessage -> sendMessage(context.id, errorMessage) }
            },
            waitDataCallbackQueryWithUser(context.id.toChatId()).map { dataCallback ->
              tryProcessGradingByButtonPress(dataCallback, academicWorkflowService)
            },
          )
          .first()
      }
    }
  }

  private fun tryParseGradingReply(
    commonMessage: CommonMessage<TextContent>,
    academicWorkflowService: AcademicWorkflowService,
  ): Result<Unit, String> = binding {
    val technicalMessageText = extractReplyText(commonMessage).bind()
    val solutionId = parseTechnicalMessageContent(technicalMessageText).bind()
    val assessment = extractAssessmentFromMessage(commonMessage).bind()
    val teacherId = TeacherId(1L)
    academicWorkflowService.assessSolution(
      solutionId,
      teacherId,
      assessment,
      LocalDateTime.now().toKotlinLocalDateTime(),
    )
  }
}
