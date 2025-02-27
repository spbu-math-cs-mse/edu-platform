package com.github.heheteam.teacherbot.states

import com.github.heheteam.commonlib.Grade
import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.api.SolutionDistributor
import com.github.heheteam.commonlib.api.SolutionId
import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.database.table.TelegramSolutionMessagesHandler
import com.github.heheteam.teacherbot.SolutionAssessor
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import com.github.michaelbull.result.toResultOr
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.contentMessageOrNull
import dev.inmo.tgbotapi.extensions.utils.textedContentOrNull
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.textsources.TextSourcesList
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.types.toChatId
import dev.inmo.tgbotapi.utils.buildEntities
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.row
import dev.inmo.tgbotapi.utils.spoiler
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun extractReplyText(commonMessage: CommonMessage<*>): Result<String, String> = binding {
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

fun parseTechnicalMessageContent(text: String): Result<SolutionGradings, String> = binding {
  val regex = Regex(".*\\[(.*)]", option = RegexOption.DOT_MATCHES_ALL)
  val match = regex.matchEntire(text).toResultOr { "Not a submission message" }.bind()
  val submissionInfoString =
    match.groups[1]?.value.toResultOr { "bad regex (no group number 1)" }.bind()
  val submissionInfo =
    runCatching { submissionInfoString.let { Json.decodeFromString<SolutionGradings>(it) } }
      .mapError { "Bad regex or failed submission format" }
      .bind()
  submissionInfo
}

fun assessSolutionFromGradingButtonContent(
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
  SolutionGradings(
    solution.id,
    GradingEntry(teacherId, assessment.grade, java.time.LocalDateTime.now().toKotlinLocalDateTime()),
  )
}

val prettyJson = Json {
  prettyPrint = true
  explicitNulls = true
}

fun createTechnicalMessageContent(submissionInfo: SolutionGradings): TextSourcesList {
  val submissionSignature = prettyJson.encodeToString(submissionInfo)
  val content = buildEntities {
    +"reply to me to grade this\n"
    spoiler("[$submissionSignature]")
  }
  return content
}

@Serializable
data class SolutionGradings(val solutionId: SolutionId, val gradingEntry: GradingEntry? = null)

@Serializable data class GradingButtonContent(val solutionId: SolutionId, val grade: Grade)

@Serializable
data class GradingEntry(val teacherId: TeacherId, val grade: Grade, val time: LocalDateTime)

suspend fun BehaviourContext.tryProcessGradingByButtonPress(
  chatId: ChatId,
  dataCallback: DataCallbackQuery,
  solutionDistributor: SolutionDistributor,
  solutionAssessor: SolutionAssessor,
  telegramSolutionMessagesHandler: TelegramSolutionMessagesHandler,
) {
  val gradingInfo =
    com.github.michaelbull.result.runCatching {
      Json.decodeFromString<GradingButtonContent>(dataCallback.data)
    }
  assessSolutionFromGradingButtonContent(gradingInfo, solutionDistributor, solutionAssessor)
    .mapError { sendMessage(chatId, "Error: $it") }
    .map { submissionInfo ->
      telegramSolutionMessagesHandler.resolveGroupMessage(submissionInfo.solutionId).map {
        technicalMessage ->
        edit(
          technicalMessage.chatId.toChatId(),
          technicalMessage.messageId,
          createTechnicalMessageContent(submissionInfo),
        )
      }
    }
}

internal fun createSolutionGradingKeyboard(solutionId: SolutionId) =
  InlineKeyboardMarkup(
    keyboard =
      matrix {
        row {
          dataButton("\uD83D\uDE80+", Json.encodeToString(GradingButtonContent(solutionId, 1)))
          dataButton("\uD83D\uDE2D-", Json.encodeToString(GradingButtonContent(solutionId, 0)))
        }
      }
  )
