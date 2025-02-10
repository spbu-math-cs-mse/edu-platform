package com.github.heheteam.teacherbot.states

import com.github.heheteam.commonlib.Grade
import com.github.heheteam.commonlib.Solution
import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.api.BotEventBus
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.SolutionDistributor
import com.github.heheteam.commonlib.api.SolutionId
import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.util.sendSolutionContent
import com.github.heheteam.commonlib.util.waitDataCallbackQueryWithUser
import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import com.github.heheteam.teacherbot.SolutionAssessor
import com.github.heheteam.teacherbot.SolutionResolver
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import com.github.michaelbull.result.toResultOr
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.edit.reply_markup.editMessageReplyMarkup
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.contentMessageOrNull
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.text
import dev.inmo.tgbotapi.extensions.utils.textedContentOrNull
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.chat.Chat
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.types.toChatId
import dev.inmo.tgbotapi.utils.RiskFeature
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.row
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ListeningForSolutionsGroupState(override val context: Chat, val courseId: CourseId) : State {
  suspend fun execute(
    bot: BehaviourContext,
    solutionResolver: SolutionResolver,
    solutionDistributor: SolutionDistributor,
    solutionAssessor: SolutionAssessor,
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
            .get() ?: false
        if (belongsToChat) {
          sendSolutionIntoGroup(solution)
        }
      }
      while (true) {
        merge(
            waitTextMessageWithUser(context.id.toChatId()).map { commonMessage ->
              processCommonMessage(commonMessage, solutionDistributor, solutionAssessor)
            },
            waitDataCallbackQueryWithUser(context.id.toChatId()).map { dataCallback ->
              processDataCallback(dataCallback, solutionDistributor, solutionAssessor)
            },
          )
          .first()
      }
    }
  }

  private suspend fun BehaviourContext.processDataCallback(
    dataCallback: DataCallbackQuery,
    solutionDistributor: SolutionDistributor,
    solutionAssessor: SolutionAssessor,
  ) {
    println("in process callback!")
    val gradingInfo =
      com.github.michaelbull.result.runCatching {
        Json.decodeFromString<GradingButtonContent>(dataCallback.data)
      }
    println("grading info: $gradingInfo")
    binding {
        val buttonInfo = gradingInfo.bind()
        val assessment = SolutionAssessment(buttonInfo.grade, "")
        val solution =
          solutionDistributor
            .resolveSolution(buttonInfo.solutionId)
            .mapError { "failed to resolve solution" }
            .bind()
        solutionAssessor.assessSolution(solution, TeacherId(1L), assessment)
      }
      .mapError { sendMessage(context.id, "Error: $it") }
  }

  @OptIn(RiskFeature::class)
  private suspend fun BehaviourContext.processCommonMessage(
    commonMessage: CommonMessage<TextContent>,
    solutionDistributor: SolutionDistributor,
    solutionAssessor: SolutionAssessor,
  ) {
    val comment = commonMessage.text.orEmpty()
    val grade = if (comment.contains("[+]")) 1 else 0
    val error =
      binding {
          val text = extractReplyText(commonMessage).bind()
          val submissionInfo = parseReplyText(text).bind()
          val solution =
            solutionDistributor
              .resolveSolution(submissionInfo.solutionId)
              .mapError { "failed to resolve solution" }
              .bind()
          val assessment = SolutionAssessment(grade, commonMessage.text.orEmpty())
          solutionAssessor.assessSolution(solution, TeacherId(1L), assessment)
        }
        .getError()
    if (error != null) {
      sendMessage(context, error)
    }
  }

  private fun parseReplyText(text: String) = binding {
    val regex = Regex(".*\\[(.*)]", option = RegexOption.DOT_MATCHES_ALL)
    val match = regex.matchEntire(text).toResultOr { "Not a submission message" }.bind()
    val submissionInfo =
      runCatching { match.groups[1]?.value!!.let { Json.decodeFromString<SubmissionInfo>(it) } }
        .mapError { "Bad regex or failed submission format" }
        .bind()
    submissionInfo
  }

  private fun extractReplyText(commonMessage: CommonMessage<TextContent>) = binding {
    val reply = commonMessage.replyTo.toResultOr { "not a reply" }.bind()
    val text =
      reply
        .contentMessageOrNull()
        ?.content
        ?.textedContentOrNull()
        ?.text
        .toResultOr { "message not a text" }
        .bind()
    text
  }

  private suspend fun BehaviourContext.sendSolutionIntoGroup(solution: Solution) {
    val submissionSignature = Json.encodeToString(SubmissionInfo(solutionId = solution.id))
    sendSolutionContent(context.id.toChatId(), solution.content)
    val technicalMessage =
      sendMessage(context.id.toChatId(), "reply to me to grade this\n[$submissionSignature]")
    editMessageReplyMarkup(
      technicalMessage,
      replyMarkup =
        InlineKeyboardMarkup(
          keyboard =
            matrix {
              row {
                dataButton(
                  "\uD83D\uDE80+",
                  Json.encodeToString(GradingButtonContent(solution.id, 1)),
                )
                dataButton(
                  "\uD83D\uDE2D-",
                  Json.encodeToString(GradingButtonContent(solution.id, 0)),
                )
              }
            }
        ),
    )
  }

  @Serializable private data class SubmissionInfo(val solutionId: SolutionId)

  @Serializable
  private data class GradingButtonContent(val solutionId: SolutionId, val grade: Grade)
}
