package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.AttachmentKind
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.SolutionAttachment
import com.github.heheteam.commonlib.SolutionContent
import com.github.heheteam.commonlib.SolutionInputRequest
import com.github.heheteam.commonlib.TelegramMessageInfo
import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.util.BotStateWithHandlers
import com.github.heheteam.commonlib.util.HandlerResultWithUserInput
import com.github.heheteam.commonlib.util.HandlingError
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.UpdateHandlersController
import com.github.heheteam.commonlib.util.UserInput
import com.github.heheteam.studentbot.Dialogues
import com.github.heheteam.studentbot.Keyboards.RETURN_BACK
import com.github.heheteam.studentbot.StudentApi
import com.github.heheteam.studentbot.metaData.back
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.bot.setMyCommands
import dev.inmo.tgbotapi.extensions.api.get.getFileAdditionalInfo
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.api.send.setMessageReaction
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.BotCommand
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.AudioContent
import dev.inmo.tgbotapi.types.message.content.DocumentContent
import dev.inmo.tgbotapi.types.message.content.MediaContent
import dev.inmo.tgbotapi.types.message.content.MediaGroupContent
import dev.inmo.tgbotapi.types.message.content.PhotoContent
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.message.content.VideoContent
import java.time.LocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime

data class SendSolutionState(
  override val context: User,
  val studentId: StudentId,
  val problem: Problem,
) : BotStateWithHandlers<SolutionInputRequest?, SolutionInputRequest?, StudentApi> {
  lateinit var studentBotToken: String

  override suspend fun intro(
    bot: BehaviourContext,
    service: StudentApi,
    updateHandlersController: UpdateHandlersController<() -> Unit, SolutionInputRequest?, Any>,
  ) {
    bot.send(context, Dialogues.tellValidSolutionTypes(), replyMarkup = back())
    bot.setMyCommands(BotCommand("menu", "main menu"))
    updateHandlersController.addTextMessageHandler { message ->
      if (message.content.text == "/menu") {
        NewState(MenuState(context, studentId))
      } else {
        Unhandled
      }
    }
    updateHandlersController.addTextMessageHandler { message ->
      bot.parseSentSolution(message, studentBotToken)
    }
    updateHandlersController.addMediaMessageHandler { message ->
      bot.setMessageReaction(message, "\uD83E\uDD23")
      bot.parseSentSolution(message, studentBotToken)
    }
    updateHandlersController.addDocumentMessageHandler { message ->
      bot.parseSentSolution(message, studentBotToken)
    }
    updateHandlersController.addDataCallbackHandler { dataCallbackQuery ->
      if (dataCallbackQuery.data == RETURN_BACK) {
        UserInput(null)
      } else {
        Unhandled
      }
    }
  }

  override fun computeNewState(
    service: StudentApi,
    input: SolutionInputRequest?,
  ): Pair<State, SolutionInputRequest?> {
    return if (input == null) {
      MenuState(context, studentId) to input
    } else {
      ConfirmSubmissionState(context, input) to input
    }
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: StudentApi,
    response: SolutionInputRequest?,
  ) = Unit

  private suspend fun BehaviourContext.parseSentSolution(
    solutionMessage: CommonMessage<*>,
    studentBotToken: String,
  ): HandlerResultWithUserInput<Nothing, SolutionInputRequest, String> {
    val attachment = extractSolutionContent(solutionMessage, studentBotToken)
    return if (attachment == null) {
      HandlingError(Dialogues.tellSolutionTypeIsInvalid())
    } else {
      UserInput(
        SolutionInputRequest(
          studentId,
          problem.id,
          attachment,
          TelegramMessageInfo(solutionMessage.chat.id.chatId, solutionMessage.messageId),
          LocalDateTime.now().toKotlinLocalDateTime(),
        )
      )
    }
  }

  override suspend fun outro(bot: BehaviourContext, service: StudentApi) = Unit
}

private suspend fun BehaviourContext.makeURL(
  content: MediaContent,
  studentBotToken: String,
): String {
  val contentInfo = bot.getFileAdditionalInfo(content)
  return "https://api.telegram.org/file/bot$studentBotToken/${contentInfo.filePath}"
}

private suspend fun BehaviourContext.extractSolutionContent(
  message: CommonMessage<*>,
  studentBotToken: String,
): SolutionContent? =
  when (val content = message.content) {
    is TextContent -> SolutionContent(content.text)
    is PhotoContent ->
      extractSingleAttachment(
        content.text.orEmpty(),
        AttachmentKind.PHOTO,
        content,
        studentBotToken,
      )

    is DocumentContent ->
      extractSingleAttachment(
        content.text.orEmpty(),
        AttachmentKind.DOCUMENT,
        content,
        studentBotToken,
      )

    is MediaGroupContent<*> -> extractMultipleAttachments(content, studentBotToken)
    else -> null
  }

private suspend fun BehaviourContext.extractSingleAttachment(
  text: String,
  attachmentKind: AttachmentKind,
  content: MediaContent,
  studentBotToken: String,
) =
  SolutionContent(
    text,
    listOf(
      SolutionAttachment(
        attachmentKind,
        makeURL(content, studentBotToken),
        content.media.fileId.fileId,
      )
    ),
  )

private suspend fun BehaviourContext.extractMultipleAttachments(
  content: MediaGroupContent<*>,
  studentBotToken: String,
): SolutionContent? =
  SolutionContent(
    content.text.orEmpty(),
    content.group.map {
      val kind =
        when (it.content) {
          is DocumentContent -> AttachmentKind.DOCUMENT
          is PhotoContent -> AttachmentKind.PHOTO
          is AudioContent -> return null
          is VideoContent -> return null
        }
      SolutionAttachment(kind, makeURL(it.content, studentBotToken), it.content.media.fileId.fileId)
    },
  )
