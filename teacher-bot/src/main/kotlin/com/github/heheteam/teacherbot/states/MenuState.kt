package com.github.heheteam.teacherbot.states

import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.TelegramMessageInfo
import com.github.heheteam.commonlib.api.SolutionId
import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.api.TeacherStorage
import com.github.heheteam.commonlib.api.TelegramTechnicalMessagesStorage
import com.github.heheteam.commonlib.api.toTeacherId
import com.github.heheteam.commonlib.logic.AcademicWorkflowService
import com.github.heheteam.commonlib.util.ActionWrapper
import com.github.heheteam.commonlib.util.HandlerResult
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.TextMessageHandler
import com.github.heheteam.commonlib.util.delete
import com.github.heheteam.commonlib.util.ok
import com.github.heheteam.commonlib.util.waitDataCallbackQueryWithUser
import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import com.github.heheteam.teacherbot.Dialogues
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import dev.inmo.kslog.common.error
import dev.inmo.kslog.common.logger
import dev.inmo.micro_utils.coroutines.firstNotNull
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.message
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.RiskFeature
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.row
import java.time.LocalDateTime
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.serialization.json.Json

class MenuState(override val context: User, val teacherId: TeacherId) : State {
  private val messages = mutableListOf<ContentMessage<*>>()

  suspend fun handle(
    bot: BehaviourContext,
    teacherStorage: TeacherStorage,
    academicWorkflowService: AcademicWorkflowService,
    technicalMessageStorage: TelegramTechnicalMessagesStorage,
  ): State {
    teacherStorage.updateTgId(teacherId, context.id)
    val stickerMessage = bot.sendSticker(context, Dialogues.typingSticker)
    val menuMessage = bot.send(context, Dialogues.menu)
    technicalMessageStorage.updateTeacherMenuMessage(
      TelegramMessageInfo(menuMessage.chat.id.chatId, menuMessage.messageId)
    )
    messages.add(stickerMessage)
    messages.add(menuMessage)

    val messageHandlers = createTextMessageHandlers()
    val datacallbackQueryHandlers = createDataCallbackHandlers()
    while (true) {
      val action =
        merge(
            bot.waitTextMessageWithUser(context.id).map { message ->
              messageHandlers.firstNotNullOfOrNull { handler -> handler.invoke(message) }
            },
            bot.waitDataCallbackQueryWithUser(context.id).map { data ->
              datacallbackQueryHandlers.firstNotNullOfOrNull { handler -> handler.invoke(data) }
            },
          )
          .firstNotNull()
      action.get()?.let {
        when (it) {
          is ActionWrapper<TeacherAction> -> executeAction(it.action, academicWorkflowService, bot)
          is NewState -> return it.state
        }
      }
      action.getError()?.let { error -> bot.send(context.id, error.toString()) }
    }
  }

  private fun createTextMessageHandlers(): List<TextMessageHandler<TeacherAction, MessageError>> =
    listOf(
      ::tryParseGradingReplyWithoutChecking,
      { message -> NewState(handleCommands(message.content.text).first).ok() },
    )

  private fun createDataCallbackHandlers() =
    listOf(::tryHandleConfirmButtonPress, ::tryHandleGradingButtonPress)

  @OptIn(RiskFeature::class)
  private fun tryHandleConfirmButtonPress(
    data: DataCallbackQuery
  ): Result<HandlerResult<TeacherAction>, Any>? {
    val number = data.data.toIntOrNull()
    return if (number != null) {
      val corresponded = storedInfo[number]
      if (corresponded != null) {
        ActionWrapper<TeacherAction>(ConfirmSending(corresponded.first, corresponded.second)).ok()
      } else null
    } else if (data.data == "no") {
      val message = data.message
      ActionWrapper<TeacherAction>(DeleteMessage(message)).ok()
    } else null
  }

  private fun tryHandleGradingButtonPress(
    dataCallback: DataCallbackQuery
  ): Result<ActionWrapper<TeacherAction>, Nothing>? =
    runCatching { Json.decodeFromString<GradingButtonContent>(dataCallback.data) }
      .map { ActionWrapper<TeacherAction>(GradingFromButton(it.solutionId, it.grade)) }
      .get()
      ?.ok()

  private var counter = 0
  private val storedInfo = mutableMapOf<Int, Pair<SolutionId, SolutionAssessment>>()

  private suspend fun executeAction(
    action: TeacherAction,
    academicWorkflowService: AcademicWorkflowService,
    bot: BehaviourContext,
  ) {
    when (action) {
      is GradingFromButton ->
        academicWorkflowService.assessSolution(
          action.solutionId,
          teacherId,
          SolutionAssessment(action.grade, ""),
          LocalDateTime.now().toKotlinLocalDateTime(),
        )
      is GradingFromReply -> {
        storedInfo[++counter] = action.solutionId to action.solutionAssessment
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

      is ConfirmSending -> {
        academicWorkflowService.assessSolution(
          action.solutionId,
          teacherId,
          action.solutionAssessment,
          LocalDateTime.now().toKotlinLocalDateTime(),
        )
      }

      is DeleteMessage -> {
        with(bot) { action.message?.let { delete(it) } }
      }
    }
  }

  private fun handleCommands(message: String): Pair<State, String?> {
    val re = Regex("/setid ([0-9]+)")
    val match = re.matchEntire(message)
    return if (match != null) {
      val newId =
        match.groups[1]?.value?.toLongOrNull()
          ?: run {
            logger.error("input id ${match.groups[1]} is not long!")
            return Pair(MenuState(context, teacherId), null)
          }
      Pair(PresetTeacherState(context, newId.toTeacherId()), null)
    } else {
      Pair(MenuState(context, teacherId), "Unrecognized command")
    }
  }

  fun tryParseGradingReplyWithoutChecking(
    commonMessage: CommonMessage<TextContent>
  ): Result<HandlerResult<TeacherAction>, MessageError> = binding {
    val technicalMessageText = extractReplyText(commonMessage).mapError { NotReply }.bind()
    val solutionId =
      parseTechnicalMessageContent(technicalMessageText).mapError { ReplyNotToSolution }.bind()
    val assessment =
      extractAssessmentFromMessage(commonMessage).mapError { BadAssessment(it) }.bind()
    ActionWrapper(GradingFromReply(solutionId, assessment))
  }
}

sealed interface MessageError

data object NotReply : MessageError

data object ReplyNotToSolution : MessageError

data class BadAssessment(val error: String) : MessageError
