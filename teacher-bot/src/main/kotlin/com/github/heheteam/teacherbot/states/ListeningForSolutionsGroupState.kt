package com.github.heheteam.teacherbot.states

import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.api.TeacherApi
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.SolutionId
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.heheteam.commonlib.util.delete
import com.github.heheteam.commonlib.util.waitDataCallbackQueryWithUser
import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.toResultOr
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.info
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.message
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.chat.Chat
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.types.toChatId
import dev.inmo.tgbotapi.utils.RiskFeature
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.row
import java.time.LocalDateTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.datetime.toKotlinLocalDateTime

class ListeningForSolutionsGroupState(override val context: Chat, val courseId: CourseId) : State {

  var lateinitTeacherBotToken: String? = null

  @OptIn(RiskFeature::class)
  suspend fun handle(bot: BehaviourContext, teacherApi: TeacherApi): State {
    with(bot) {
      teacherApi.setCourseGroup(courseId, context.id.chatId)
      teacherApi.updateGroupMenuMessage(courseId)
      while (true) {
        merge(
            waitTextMessageWithUser(context.id.toChatId()).map { commonMessage ->
              if (commonMessage.content.text == "/menu") {
                teacherApi.updateGroupMenuMessage(courseId)
                return@map
              }
              val result = tryParseGradingReply(commonMessage, bot)
              result.mapError { errorMessage -> sendMessage(context.id, errorMessage) }
            },
            waitDataCallbackQueryWithUser(context.id.toChatId()).map { dataCallback ->
              val maybeCounter = dataCallback.data.toIntOrNull()
              if (maybeCounter != null) {
                tryProcessConfirmingAssessment(maybeCounter, teacherApi, dataCallback)
              } else if (dataCallback.data == "no") {
                with(bot) { dataCallback.message?.let { delete(it) } }
              } else {
                tryProcessGradingByButtonPress(dataCallback, teacherApi)
              }
            },
          )
          .first()
      }
    }
  }

  @OptIn(RiskFeature::class)
  private suspend fun BehaviourContext.tryProcessConfirmingAssessment(
    maybeCounter: Int?,
    teacherApi: TeacherApi,
    dataCallback: DataCallbackQuery,
  ): Unit? {
    val data = storedInfo[maybeCounter]
    return if (data != null) {
      teacherApi.assessSolution(
        data.first,
        TeacherId(1L),
        data.second,
        LocalDateTime.now().toKotlinLocalDateTime(),
      )
      dataCallback.message?.let { delete(it) }
    } else {
      KSLog.info("null")
    }
  }

  private var counter = 0
  private val storedInfo = mutableMapOf<Int, Pair<SolutionId, SolutionAssessment>>()

  private suspend fun tryParseGradingReply(
    commonMessage: CommonMessage<*>,
    bot: BehaviourContext,
  ): Result<Unit, String> = coroutineBinding {
    val teacherBotToken =
      lateinitTeacherBotToken
        .toResultOr { "Uninitialized teacher bot token in ListeningForSolutionGradeState" }
        .bind()
    val technicalMessageText = extractReplyText(commonMessage).bind()
    val solutionId = parseTechnicalMessageContent(technicalMessageText).bind()
    val assessment = extractAssessmentFromMessage(commonMessage, teacherBotToken, bot).bind()
    storedInfo[++counter] = solutionId to assessment
    bot.sendMessage(
      context.id,
      "Вы подтверждаете отправку?",
      replyMarkup =
        InlineKeyboardMarkup(
          keyboard =
            matrix {
              row { dataButton("Да", counter.toString()) }
              row { dataButton("No", "no") }
            }
        ),
    )
  }
}
