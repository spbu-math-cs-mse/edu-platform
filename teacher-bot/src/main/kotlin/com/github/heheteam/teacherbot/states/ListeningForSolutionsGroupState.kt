package com.github.heheteam.teacherbot.states

import com.github.heheteam.commonlib.Solution
import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.api.BotEventBus
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.SolutionDistributor
import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.database.table.TelegramMessageInfo
import com.github.heheteam.commonlib.database.table.TelegramSolutionMessagesHandler
import com.github.heheteam.commonlib.util.sendSolutionContent
import com.github.heheteam.commonlib.util.waitDataCallbackQueryWithUser
import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import com.github.heheteam.teacherbot.SolutionAssessor
import com.github.heheteam.teacherbot.SolutionResolver
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.getOr
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.api.edit.reply_markup.editMessageReplyMarkup
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.text
import dev.inmo.tgbotapi.types.chat.Chat
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.abstracts.Message
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.toChatId
import dev.inmo.tgbotapi.utils.RiskFeature
import java.time.LocalDateTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.datetime.toKotlinLocalDateTime

class ListeningForSolutionsGroupState(override val context: Chat, val courseId: CourseId) : State {
  suspend fun execute(
    bot: BehaviourContext,
    solutionResolver: SolutionResolver,
    solutionDistributor: SolutionDistributor,
    solutionAssessor: SolutionAssessor,
    telegramSolutionMessagesHandler: TelegramSolutionMessagesHandler,
    bus: BotEventBus,
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
              val result =
                tryParseGradingReply(commonMessage, solutionDistributor, solutionAssessor)
              result.mapError { errorMessage -> sendMessage(context.id, errorMessage) }
            },
            waitDataCallbackQueryWithUser(context.id.toChatId()).map { dataCallback ->
              tryProcessGradingByButtonPress(
                context.id.toChatId(),
                dataCallback,
                solutionDistributor,
                solutionAssessor,
                telegramSolutionMessagesHandler,
              )
            },
          )
          .first()
      }
    }
  }

  suspend fun BehaviourContext.tryParseGradingReply(
    commonMessage: CommonMessage<TextContent>,
    solutionDistributor: SolutionDistributor,
    solutionAssessor: SolutionAssessor,
  ): Result<Unit, String> =
    binding {
        val technicalMessageText = extractReplyText(commonMessage).bind()
        val submissionInfo = parseTechnicalMessageContent(technicalMessageText).bind()
        val solution =
          solutionDistributor
            .resolveSolution(submissionInfo.solutionId)
            .mapError { "failed to resolve solution" }
            .bind()
        val assessment = extractAssessmentFromMessage(commonMessage).bind()
        val teacherId = TeacherId(1L)
        solutionAssessor.assessSolution(solution, teacherId, assessment, LocalDateTime.now())
        SolutionGradings(
          solution.id,
          GradingEntry(teacherId, assessment.grade, LocalDateTime.now().toKotlinLocalDateTime()),
        )
      }
      .map { solutionGradings ->
        val technicalMessage = commonMessage.replyTo
        if (technicalMessage != null) {
          updateTechnicalMessage(technicalMessage, solutionGradings)
        }
      }

  private suspend fun BehaviourContext.updateTechnicalMessage(
    technicalMessage: Message,
    solutionGradings: SolutionGradings,
  ) {
    edit(
      technicalMessage.chat.id.toChatId(),
      technicalMessage.messageId,
      createTechnicalMessageContent(solutionGradings),
    )
  }

  @OptIn(RiskFeature::class)
  private fun extractAssessmentFromMessage(
    commonMessage: CommonMessage<TextContent>
  ): Result<SolutionAssessment, String> = binding {
    val comment = commonMessage.text.orEmpty()
    val grade =
      when {
        comment.contains("[+]") -> Ok(1)
        comment.contains("[-]") -> Ok(0)
        else -> Err("message must contain [+] or [-] to be graded")
      }.bind()
    SolutionAssessment(grade, commonMessage.text.orEmpty())
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
