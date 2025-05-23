package com.github.heheteam.teacherbot.states

import com.github.heheteam.commonlib.Grade
import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.api.TeacherApi
import com.github.heheteam.commonlib.interfaces.SolutionId
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.heheteam.commonlib.util.extractTextWithMediaAttachments
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import com.github.michaelbull.result.toResultOr
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.utils.contentMessageOrNull
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.text
import dev.inmo.tgbotapi.extensions.utils.textedContentOrNull
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.serialization.Serializable
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

fun parseTechnicalMessageContent(text: String): Result<SolutionId, String> = binding {
  val regex = Regex("Отправка #(\\d+)", option = RegexOption.DOT_MATCHES_ALL)
  val match = regex.find(text).toResultOr { "Not a submission message" }.bind()
  val solutionIdString =
    match.groups[1]?.value.toResultOr { "bad regex (no group number 1)" }.bind()
  val solutionId =
    solutionIdString
      .let { it.toLongOrNull().toResultOr { Unit } }
      .mapError { "Bad regex or failed submission format" }
      .bind()
  SolutionId(solutionId)
}

@Serializable data class GradingButtonContent(val solutionId: SolutionId, val grade: Grade)

fun tryProcessGradingByButtonPress(
  dataCallback: DataCallbackQuery,
  teacherApi: TeacherApi,
  teacherId: TeacherId = TeacherId(1L),
) = binding {
  val gradingButtonContent =
    runCatching { Json.decodeFromString<GradingButtonContent>(dataCallback.data) }.bind()
  teacherApi.assessSolution(
    gradingButtonContent.solutionId,
    teacherId,
    SolutionAssessment(gradingButtonContent.grade),
    java.time.LocalDateTime.now().toKotlinLocalDateTime(),
  )
}

internal suspend fun extractAssessmentFromMessage(
  commonMessage: CommonMessage<*>,
  teacherBotToken: String,
  telegramBot: TelegramBot,
): Result<SolutionAssessment, String> = coroutineBinding {
  val content =
    extractTextWithMediaAttachments(commonMessage, teacherBotToken, telegramBot)
      .toResultOr { "Unsupported attachment type" }
      .bind()
  val comment = content.text
  val grade =
    when {
      comment.contains("[+]") -> Ok(1)
      comment.contains("[-]") -> Ok(0)
      else -> Err("message must contain [+] or [-] to be graded")
    }.bind()
  SolutionAssessment(grade, content)
}
