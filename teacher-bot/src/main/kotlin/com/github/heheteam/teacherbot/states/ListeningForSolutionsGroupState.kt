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
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.getError
import com.github.michaelbull.result.getOr
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import com.github.michaelbull.result.toResultOr
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.api.edit.reply_markup.editMessageReplyMarkup
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.contentMessageOrNull
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.message
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.text
import dev.inmo.tgbotapi.extensions.utils.textedContentOrNull
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.chat.Chat
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.abstracts.Message
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.message.textsources.TextSourcesList
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.types.toChatId
import dev.inmo.tgbotapi.utils.RiskFeature
import dev.inmo.tgbotapi.utils.buildEntities
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.row
import dev.inmo.tgbotapi.utils.spoiler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
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
            .getOr(false)
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

  @OptIn(RiskFeature::class)
  private suspend fun BehaviourContext.processDataCallback(
    dataCallback: DataCallbackQuery,
    solutionDistributor: SolutionDistributor,
    solutionAssessor: SolutionAssessor,
  ) {
    val gradingInfo =
      com.github.michaelbull.result.runCatching {
        Json.decodeFromString<GradingButtonContent>(dataCallback.data)
      }
    assessSolutionFromButtonPress(gradingInfo, solutionDistributor, solutionAssessor)
      .mapError { sendMessage(context.id, "Error: $it") }
      .map { submissionInfo ->
        val message = dataCallback.message
        if (message != null) {
          edit(
            message.chat.id.toChatId(),
            message.messageId,
            createTechnicalMessageContent(submissionInfo),
          )
        }
      }
  }

  private fun assessSolutionFromButtonPress(
    gradingInfo: Result<GradingButtonContent, Throwable>,
    solutionDistributor: SolutionDistributor,
    solutionAssessor: SolutionAssessor,
  ) = binding {
    val buttonInfo = gradingInfo.bind()
    val assessment = SolutionAssessment(buttonInfo.grade, "")
    val solution =
      solutionDistributor
        .resolveSolution(buttonInfo.solutionId)
        .mapError { "failed to resolve solution" }
        .bind()
    val teacherId = TeacherId(1L)
    solutionAssessor.assessSolution(solution, teacherId, assessment, java.time.LocalDateTime.now())
    SubmissionInfo(
      solution.id,
      GradingInfo(
        teacherId,
        assessment.grade,
        java.time.LocalDateTime.now().toKotlinLocalDateTime(),
      ),
    )
  }

  private suspend fun BehaviourContext.processCommonMessage(
    commonMessage: CommonMessage<TextContent>,
    solutionDistributor: SolutionDistributor,
    solutionAssessor: SolutionAssessor,
  ) {
    val error =
      binding {
          val technicalMessageText = extractReplyText(commonMessage).bind()
          val submissionInfo = parseTechnicalMessage(technicalMessageText).bind()
          val solution =
            solutionDistributor
              .resolveSolution(submissionInfo.solutionId)
              .mapError { "failed to resolve solution" }
              .bind()
          val assessment = extractAssesmentFromMessage(commonMessage).bind()
          val teacherId = TeacherId(1L)
          solutionAssessor.assessSolution(
            solution,
            teacherId,
            assessment,
            java.time.LocalDateTime.now(),
          )
          SubmissionInfo(
            solution.id,
            GradingInfo(
              teacherId,
              assessment.grade,
              java.time.LocalDateTime.now().toKotlinLocalDateTime(),
            ),
          )
        }
        .map { submissionInfo -> updateTechnicalMessage(commonMessage.replyTo, submissionInfo) }
        .getError()
    if (error != null) {
      sendMessage(context, error)
    }
  }

  private suspend fun BehaviourContext.updateTechnicalMessage(
    technicalMessage: Message?,
    submissionInfo: SubmissionInfo,
  ) {
    if (technicalMessage != null) {
      edit(
        technicalMessage.chat.id.toChatId(),
        technicalMessage.messageId,
        createTechnicalMessageContent(submissionInfo),
      )
    }
  }

  @OptIn(RiskFeature::class)
  private fun extractAssesmentFromMessage(commonMessage: CommonMessage<TextContent>) = binding {
    val comment = commonMessage.text.orEmpty()
    val grade =
      when {
        comment.contains("[+]") -> Ok(1)
        comment.contains("[-]") -> Ok(0)
        else -> Err("message must contain [+] or [-] to be graded")
      }.bind()
    SolutionAssessment(grade, commonMessage.text.orEmpty())
  }

  private fun parseTechnicalMessage(text: String) = binding {
    val regex = Regex(".*\\[(.*)]", option = RegexOption.DOT_MATCHES_ALL)
    val match = regex.matchEntire(text).toResultOr { "Not a submission message" }.bind()
    val submissionInfoString =
      match.groups[1]?.value.toResultOr { "bad regex (no group number 1)" }.bind()
    val submissionInfo =
      runCatching { submissionInfoString.let { Json.decodeFromString<SubmissionInfo>(it) } }
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

  private val prettyJson = Json {
    prettyPrint = true
    explicitNulls = true
  }

  private suspend fun BehaviourContext.sendSolutionIntoGroup(solution: Solution) {
    val solutionMessage = sendSolutionContent(context.id.toChatId(), solution.content)
    val submissionInfo = SubmissionInfo(solutionId = solution.id)
    val content = createTechnicalMessageContent(submissionInfo)
    val technicalMessage = reply(solutionMessage, content)
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

  private fun createTechnicalMessageContent(submissionInfo: SubmissionInfo): TextSourcesList {
    val submissionSignature = prettyJson.encodeToString(submissionInfo)
    val content = buildEntities {
      +"reply to me to grade this\n"
      spoiler("[$submissionSignature]")
    }
    return content
  }

  @Serializable
  private data class SubmissionInfo(
    val solutionId: SolutionId,
    val gradingInfo: GradingInfo? = null,
  )

  @Serializable
  private data class GradingButtonContent(val solutionId: SolutionId, val grade: Grade)

  @Serializable
  private data class GradingInfo(
    val teacherId: TeacherId,
    val grade: Grade,
    val time: LocalDateTime,
  )
}
