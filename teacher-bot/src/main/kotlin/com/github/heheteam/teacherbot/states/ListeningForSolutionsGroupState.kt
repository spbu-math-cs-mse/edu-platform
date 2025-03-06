package com.github.heheteam.teacherbot.states

import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.util.waitDataCallbackQueryWithUser
import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import com.github.heheteam.teacherbot.logic.SolutionGrader
import com.github.heheteam.teacherbot.logic.TelegramSolutionSenderImpl
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

class ListeningForSolutionsGroupState(override val context: Chat, val courseId: CourseId) : State {
  suspend fun execute(
    bot: BehaviourContext,
    solutionGrader: SolutionGrader,
    telegramSolutionSenderImpl: TelegramSolutionSenderImpl,
  ): State {
    with(bot) {
      telegramSolutionSenderImpl.registerGroupForSolution(courseId, context.id.chatId)
      while (true) {
        merge(
            waitTextMessageWithUser(context.id.toChatId()).map { commonMessage ->
              val result = tryParseGradingReply(commonMessage, solutionGrader)
              result.mapError { errorMessage -> sendMessage(context.id, errorMessage) }
            },
            waitDataCallbackQueryWithUser(context.id.toChatId()).map { dataCallback ->
              tryProcessGradingByButtonPress(dataCallback, solutionGrader)
            },
          )
          .first()
      }
    }
  }

  fun tryParseGradingReply(
    commonMessage: CommonMessage<TextContent>,
    solutionGrader: SolutionGrader,
  ): Result<Unit, String> = binding {
    val technicalMessageText = extractReplyText(commonMessage).bind()
    val solutionId = parseTechnicalMessageContent(technicalMessageText).bind()
    val assessment = extractAssessmentFromMessage(commonMessage).bind()
    val teacherId = TeacherId(1L)
    solutionGrader.assessSolution(solutionId, teacherId, assessment, LocalDateTime.now())
  }
}
