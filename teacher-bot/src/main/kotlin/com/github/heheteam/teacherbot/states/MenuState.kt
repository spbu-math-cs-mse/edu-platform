package com.github.heheteam.teacherbot.states

import com.github.heheteam.commonlib.api.SolutionDistributor
import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.api.TeacherStorage
import com.github.heheteam.commonlib.api.toTeacherId
import com.github.heheteam.commonlib.database.table.TelegramSolutionMessagesHandler
import com.github.heheteam.commonlib.util.waitDataCallbackQueryWithUser
import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import com.github.heheteam.teacherbot.Dialogues
import com.github.heheteam.teacherbot.Keyboards
import com.github.heheteam.teacherbot.SolutionAssessor
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.get
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import dev.inmo.kslog.common.error
import dev.inmo.kslog.common.logger
import dev.inmo.micro_utils.coroutines.firstNotNull
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.api.edit.reply_markup.editMessageReplyMarkup
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.abstracts.Message
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.toChatId
import java.time.LocalDateTime
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.datetime.toKotlinLocalDateTime

class MenuState(override val context: User, val teacherId: TeacherId) : State {
  private val messages = mutableListOf<ContentMessage<*>>()
  private val epilogueMessage: String? = null

  suspend fun readUserInput(
    bot: BehaviourContext,
    service: TeacherStorage,
    solutionDistributor: SolutionDistributor,
    solutionAssessor: SolutionAssessor,
    telegramSolutionMessagesHandler: TelegramSolutionMessagesHandler,
  ): Pair<State, String?> {
    val result = service.updateTgId(teacherId, context.id)
    if (context.username == null) {
      return Pair(StartState(context), null)
    }
    val stickerMessage = bot.sendSticker(context, Dialogues.typingSticker)
    val menuMessage = bot.send(context, Dialogues.menu(), replyMarkup = Keyboards.menu())
    messages.add(stickerMessage)
    messages.add(menuMessage)

    val callbacksFlow =
      bot.waitDataCallbackQueryWithUser(context.id).map { callback ->
        val tryGrading =
          tryProcessGradingByButtonPress(
              callback,
              solutionDistributor,
              solutionAssessor,
              telegramSolutionMessagesHandler,
            )
            .get()
        if (tryGrading != null) {
          val solutionGradings = tryGrading
          val messageContent = createTechnicalMessageContent(solutionGradings)
          telegramSolutionMessagesHandler.resolveGroupMessage(solutionGradings.solutionId).map {
            groupTechnicalMessage ->
            bot.edit(
              groupTechnicalMessage.chatId.toChatId(),
              groupTechnicalMessage.messageId,
              messageContent,
            )
            bot.editMessageReplyMarkup(
              groupTechnicalMessage.chatId.toChatId(),
              groupTechnicalMessage.messageId,
              replyMarkup = createSolutionGradingKeyboard(solutionGradings.solutionId),
            )
          }
          telegramSolutionMessagesHandler.resolvePersonalMessage(solutionGradings.solutionId).map {
            personalTechnicalMessage ->
            bot.edit(
              personalTechnicalMessage.chatId.toChatId(),
              personalTechnicalMessage.messageId,
              messageContent,
            )
            bot.editMessageReplyMarkup(
              personalTechnicalMessage.chatId.toChatId(),
              personalTechnicalMessage.messageId,
              replyMarkup = createSolutionGradingKeyboard(solutionGradings.solutionId),
            )
          }
          null
        } else {
          Pair(handleDataCallbackFromMenuButtons(callback.data), null)
        }
      }
    val messagesFlow =
      bot.waitTextMessageWithUser(context.id).map { message ->
        val data =
          with(bot) {
            val result =
              tryParseGradingReply(
                message,
                solutionDistributor,
                solutionAssessor,
                telegramSolutionMessagesHandler,
              )
            result.mapError { errorMessage -> sendMessage(context.id, errorMessage) }
          }
        if (data.isOk) {
          null
        } else {
          handleTextMessage(message.content.text)
        }
      }
    return merge(callbacksFlow, messagesFlow).firstNotNull()
  }

  suspend fun handle(
    bot: BehaviourContext,
    teacherStorage: TeacherStorage,
    solutionDistributor: SolutionDistributor,
    solutionAssessor: SolutionAssessor,
    telegramSolutionMessagesHandler: TelegramSolutionMessagesHandler,
  ): State =
    with(bot) {
      val input =
        readUserInput(
          this,
          teacherStorage,
          solutionDistributor,
          solutionAssessor,
          telegramSolutionMessagesHandler,
        )
      sendResponse(this)
      input.first
    }

  fun computeNewState(service: TeacherStorage, input: Pair<State, String?>): Pair<State, String?> {
    return input
  }

  suspend fun sendResponse(bot: BehaviourContext) {
    messages.forEach { bot.deleteMessage(context, it.messageId) }
    if (epilogueMessage != null) bot.send(context, epilogueMessage)
  }

  private fun handleTextMessage(message: String): Pair<State, String?> {
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

  private fun handleDataCallbackFromMenuButtons(callback: String): State =
    when (callback) {
      Keyboards.checkGrades -> CheckGradesState(context, teacherId)
      Keyboards.getSolution -> GettingSolutionState(context, teacherId)
      Keyboards.viewStats -> SendStatisticInfoState(context, teacherId)
      else -> GettingSolutionState(context, teacherId)
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

  suspend fun BehaviourContext.tryParseGradingReply(
    commonMessage: CommonMessage<TextContent>,
    solutionDistributor: SolutionDistributor,
    solutionAssessor: SolutionAssessor,
    telegramSolutionMessagesHandler: TelegramSolutionMessagesHandler,
  ): Result<Unit, String> =
    binding {
        val technicalMessageText = extractReplyText(commonMessage).bind()
        val oldSolutionGradings = parseTechnicalMessageContent(technicalMessageText).bind()
        val solution =
          solutionDistributor
            .resolveSolution(oldSolutionGradings.solutionId)
            .mapError { "failed to resolve solution" }
            .bind()
        val assessment = extractAssessmentFromMessage(commonMessage).bind()
        val teacherId = TeacherId(1L)
        solutionAssessor.assessSolution(solution, teacherId, assessment, LocalDateTime.now())
        SolutionGradings(
          solution.id,
          oldSolutionGradings.gradingEntries +
            listOf(
              GradingEntry(teacherId, assessment.grade, LocalDateTime.now().toKotlinLocalDateTime())
            ),
        )
      }
      .map { solutionGradings ->
        val messageContent = createTechnicalMessageContent(solutionGradings)
        telegramSolutionMessagesHandler.resolveGroupMessage(solutionGradings.solutionId).map {
          groupTechnicalMessage ->
          edit(
            groupTechnicalMessage.chatId.toChatId(),
            groupTechnicalMessage.messageId,
            messageContent,
          )
          editMessageReplyMarkup(
            groupTechnicalMessage.chatId.toChatId(),
            groupTechnicalMessage.messageId,
            replyMarkup = createSolutionGradingKeyboard(solutionGradings.solutionId),
          )
        }
        telegramSolutionMessagesHandler.resolvePersonalMessage(solutionGradings.solutionId).map {
          personalTechnicalMessage ->
          edit(
            personalTechnicalMessage.chatId.toChatId(),
            personalTechnicalMessage.messageId,
            messageContent,
          )
          editMessageReplyMarkup(
            personalTechnicalMessage.chatId.toChatId(),
            personalTechnicalMessage.messageId,
            replyMarkup = createSolutionGradingKeyboard(solutionGradings.solutionId),
          )
        }
      }
}
