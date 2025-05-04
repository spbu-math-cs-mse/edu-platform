package com.github.heheteam.teacherbot.states

import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.api.TeacherApi
import com.github.heheteam.commonlib.interfaces.SolutionId
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.heheteam.commonlib.interfaces.toTeacherId
import com.github.heheteam.commonlib.util.ActionWrapper
import com.github.heheteam.commonlib.util.AnyMessageHandler
import com.github.heheteam.commonlib.util.HandlerResult
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.delete
import com.github.heheteam.commonlib.util.ok
import com.github.heheteam.commonlib.util.waitDataCallbackQueryWithUser
import com.github.heheteam.commonlib.util.waitDocumentMessageWithUser
import com.github.heheteam.commonlib.util.waitMediaMessageWithUser
import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import com.github.heheteam.teacherbot.Dialogues
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import dev.inmo.kslog.common.error
import dev.inmo.kslog.common.logger
import dev.inmo.micro_utils.coroutines.firstNotNull
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.message
import dev.inmo.tgbotapi.extensions.utils.textContentOrNull
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.RiskFeature
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.row
import java.time.LocalDateTime
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.serialization.json.Json

private const val NOT_CONFIRM_ASSESSING = "no"

class MenuState(override val context: User, private val teacherId: TeacherId) : State {
  private val messages = mutableListOf<ContentMessage<*>>()

  lateinit var teacherBotToken: String

  suspend fun handle(bot: BehaviourContext, teacherApi: TeacherApi): State {
    teacherApi.updateTgId(teacherId, context.id)
    val stickerMessage = bot.sendSticker(context, Dialogues.typingSticker)
    messages.add(stickerMessage)
    teacherApi.updateTeacherMenuMessage(teacherId)

    val messageHandlers = createMessagesHandlers(bot)
    val dataCallbackQueryHandlers = createDataCallbackHandlers()
    while (true) {
      val action =
        merge(
            bot.waitTextMessageWithUser(context.id).map { message ->
              messageHandlers.firstNotNullOfOrNull { handler -> handler.invoke(message) }
            },
            bot.waitMediaMessageWithUser(context.id).map { message ->
              messageHandlers.firstNotNullOfOrNull { handler -> handler.invoke(message) }
            },
            bot.waitDocumentMessageWithUser(context.id).map { message ->
              messageHandlers.firstNotNullOfOrNull { handler -> handler.invoke(message) }
            },
            bot.waitDataCallbackQueryWithUser(context.id).map { data ->
              dataCallbackQueryHandlers.firstNotNullOfOrNull { handler -> handler.invoke(data) }
            },
          )
          .firstNotNull()
      action.get()?.let {
        when (it) {
          is ActionWrapper<TeacherAction> -> executeAction(it.action, teacherApi, bot)
          is NewState -> return it.state
        }
      }
      action.getError()?.let { error -> bot.send(context.id, error.toString()) }
    }
  }

  private fun createMessagesHandlers(
    bot: TelegramBot
  ): List<AnyMessageHandler<TeacherAction, MessageError>> =
    listOf(
      { tryHandleSetIdCommand(it) },
      { tryHandleMenuCommand(it) },
      { tryParseGradingReplyWithoutChecking(it, teacherBotToken, bot) },
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
        ActionWrapper<TeacherAction>(
            ConfirmSending(corresponded.first, corresponded.second, data.message)
          )
          .ok()
      } else null
    } else if (data.data == NOT_CONFIRM_ASSESSING) {
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
    teacherApi: TeacherApi,
    bot: BehaviourContext,
  ) {
    when (action) {
      is GradingFromButton ->
        teacherApi.assessSolution(
          action.solutionId,
          teacherId,
          SolutionAssessment(action.grade),
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
                  row { dataButton("Нет", NOT_CONFIRM_ASSESSING) }
                }
            ),
        )
      }

      is ConfirmSending -> {
        teacherApi.assessSolution(
          action.solutionId,
          teacherId,
          action.solutionAssessment,
          LocalDateTime.now().toKotlinLocalDateTime(),
        )
        with(bot) { action.messageToDeleteOnConfirm?.let { delete(it) } }
      }

      is DeleteMessage -> with(bot) { action.message?.let { delete(it) } }

      is UpdateMenuMessage -> teacherApi.updateTeacherMenuMessage(teacherId)
    }
  }

  private fun tryHandleMenuCommand(
    message: CommonMessage<MessageContent>
  ): Result<ActionWrapper<TeacherAction>, Nothing>? =
    if (message.content.textContentOrNull()?.text == "/menu") {
      ActionWrapper<TeacherAction>(UpdateMenuMessage).ok()
    } else null

  private fun tryHandleSetIdCommand(
    message: CommonMessage<MessageContent>
  ): Result<NewState, Nothing>? {
    val re = Regex("/setid ([0-9]+)")
    val match = message.content.textContentOrNull()?.text?.let { re.matchEntire(it) }
    return if (match != null) {
      val newId =
        match.groups[1]?.value?.toLongOrNull()
          ?: run {
            logger.error("input id ${match.groups[1]} is not long!")
            return NewState(MenuState(context, teacherId)).ok()
          }
      NewState(PresetTeacherState(context, newId.toTeacherId())).ok()
    } else null
  }

  private fun tryParseGradingReplyWithoutChecking(
    commonMessage: CommonMessage<*>,
    teacherBotToken: String,
    telegramBot: TelegramBot,
  ): Result<HandlerResult<TeacherAction>, MessageError> = runBlocking {
    coroutineBinding {
      val technicalMessageText =
        extractReplyText(commonMessage).mapError { NotReplyOrReplyNotToTextMessage }.bind()
      val solutionId =
        parseTechnicalMessageContent(technicalMessageText).mapError { ReplyNotToSolution }.bind()
      val assessment =
        extractAssessmentFromMessage(commonMessage, teacherBotToken, telegramBot)
          .mapError { BadAssessment(it) }
          .bind()
      ActionWrapper(GradingFromReply(solutionId, assessment))
    }
  }
}

sealed interface MessageError

data object NotReplyOrReplyNotToTextMessage : MessageError

data object ReplyNotToSolution : MessageError

data class BadAssessment(val error: String) : MessageError
