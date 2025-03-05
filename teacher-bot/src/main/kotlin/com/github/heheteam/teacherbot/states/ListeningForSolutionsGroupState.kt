package com.github.heheteam.teacherbot.states

import com.github.heheteam.commonlib.Solution
import com.github.heheteam.commonlib.api.BotEventBus
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.database.table.TelegramMessageInfo
import com.github.heheteam.commonlib.database.table.TelegramSolutionMessagesHandler
import com.github.heheteam.commonlib.util.sendSolutionContent
import com.github.heheteam.commonlib.util.waitDataCallbackQueryWithUser
import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import com.github.heheteam.teacherbot.SolutionResolver
import com.github.heheteam.teacherbot.logic.SolutionGrader
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.getOr
import com.github.michaelbull.result.mapError
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.edit.reply_markup.editMessageReplyMarkup
import dev.inmo.tgbotapi.extensions.api.send.reply
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
    solutionResolver: SolutionResolver,
    telegramSolutionMessagesHandler: TelegramSolutionMessagesHandler,
    bus: BotEventBus,
    solutionGrader: SolutionGrader,
  ): State {
    with(bot) {
      bus.subscribeToNewSolutionEvent { solution: Solution ->
        val belongsToChat =
          binding {
              val problem = solutionResolver.resolveProblem(solution.problemId).bind()
              val assignment = solutionResolver.resolveAssignment(problem.assignmentId).bind()
              assignment.courseId == courseId
            }
            .getOr(false)
        if (belongsToChat) {
          sendSolutionIntoGroup(solution, telegramSolutionMessagesHandler)
        }
      }
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
    val submissionInfo = parseTechnicalMessageContent(technicalMessageText).bind()
    val assessment = extractAssessmentFromMessage(commonMessage).bind()
    val teacherId = TeacherId(1L)
    solutionGrader.assessSolution(
      submissionInfo.solutionId,
      teacherId,
      assessment,
      LocalDateTime.now(),
    )
  }

  private suspend fun BehaviourContext.sendSolutionIntoGroup(
    solution: Solution,
    telegramSolutionMessagesHandler: TelegramSolutionMessagesHandler,
  ) {
    val solutionMessage = sendSolutionContent(context.id.toChatId(), solution.content)
    val solutionGradings = SolutionGradings(solutionId = solution.id)
    val content = createTechnicalMessageContent(solutionGradings)
    val technicalMessage = reply(solutionMessage, content)
    telegramSolutionMessagesHandler.registerGroupSolutionPublication(
      solution.id,
      TelegramMessageInfo(technicalMessage.chat.id.chatId, technicalMessage.messageId),
    )
    editMessageReplyMarkup(
      technicalMessage,
      replyMarkup = createSolutionGradingKeyboard(solution.id),
    )
  }
}
