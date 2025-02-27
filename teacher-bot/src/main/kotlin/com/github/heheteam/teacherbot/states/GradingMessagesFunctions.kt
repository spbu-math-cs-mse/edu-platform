package com.github.heheteam.teacherbot.states

import com.github.heheteam.commonlib.Grade
import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.api.SolutionDistributor
import com.github.heheteam.commonlib.api.SolutionId
import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.database.table.TelegramSolutionMessagesHandler
import com.github.heheteam.teacherbot.SolutionAssessor
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import com.github.michaelbull.result.toResultOr
import dev.inmo.tgbotapi.extensions.utils.contentMessageOrNull
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.text
import dev.inmo.tgbotapi.extensions.utils.textedContentOrNull
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.message.textsources.TextSourcesList
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.RiskFeature
import dev.inmo.tgbotapi.utils.buildEntities
import dev.inmo.tgbotapi.utils.code
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.row
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
  val regex = Regex(".*\\<(.*)>", option = RegexOption.DOT_MATCHES_ALL)
  val match = regex.matchEntire(text).toResultOr { "Not a submission message" }.bind()
  val submissionInfoString =
    match.groups[1]?.value.toResultOr { "bad regex (no group number 1)" }.bind()
  val solutionGradings =
    runCatching { submissionInfoString.let { Json.decodeFromString<SolutionGradings>(it) } }
      .mapError { "Bad regex or failed submission format" }
      .bind()
  solutionGradings
}

fun assessSolutionFromGradingButtonContent(
  gradingInfo: GradingButtonContent,
  solutionDistributor: SolutionDistributor,
  solutionAssessor: SolutionAssessor,
) = binding {
  val buttonInfo = gradingInfo
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
    listOf(
      GradingEntry(
        teacherId,
        assessment.grade,
        java.time.LocalDateTime.now().toKotlinLocalDateTime(),
      )
    ),
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
    code("<$submissionSignature>")
  }
  return content
}

@Serializable
data class SolutionGradings(
  val solutionId: SolutionId,
  val gradingEntries: List<GradingEntry> = listOf(),
)

@Serializable data class GradingButtonContent(val solutionId: SolutionId, val grade: Grade)

@Serializable
data class GradingEntry(val teacherId: TeacherId, val grade: Grade, val time: LocalDateTime)

fun tryProcessGradingByButtonPress(
  dataCallback: DataCallbackQuery,
  solutionDistributor: SolutionDistributor,
  solutionAssessor: SolutionAssessor,
  telegramSolutionMessagesHandler: TelegramSolutionMessagesHandler,
) = binding {
  val gradingButtonContent =
    runCatching { Json.decodeFromString<GradingButtonContent>(dataCallback.data) }.bind()
  val solutionGradings =
    assessSolutionFromGradingButtonContent(
        gradingButtonContent,
        solutionDistributor,
        solutionAssessor,
      )
      .bind()
  solutionGradings
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

@OptIn(RiskFeature::class)
internal fun extractAssessmentFromMessage(
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
