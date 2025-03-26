package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.AttachmentKind
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.Solution
import com.github.heheteam.commonlib.SolutionAttachment
import com.github.heheteam.commonlib.SolutionContent
import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.api.toSolutionId
import com.github.heheteam.commonlib.util.isDeadlineMissed
import com.github.heheteam.commonlib.util.waitDataCallbackQueryWithUser
import com.github.heheteam.commonlib.util.waitDocumentMessageWithUser
import com.github.heheteam.commonlib.util.waitMediaMessageWithUser
import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import com.github.heheteam.studentbot.Dialogues
import com.github.heheteam.studentbot.Keyboards.RETURN_BACK
import com.github.heheteam.studentbot.metaData.back
import dev.inmo.micro_utils.coroutines.filterNotNull
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.get.getFileAdditionalInfo
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.AudioContent
import dev.inmo.tgbotapi.types.message.content.DocumentContent
import dev.inmo.tgbotapi.types.message.content.MediaContent
import dev.inmo.tgbotapi.types.message.content.MediaGroupContent
import dev.inmo.tgbotapi.types.message.content.PhotoContent
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.message.content.VideoContent
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.datetime.toKotlinLocalDateTime

data class SendSolutionState(
  override val context: User,
  val studentId: StudentId,
  val problem: Problem,
) : State

@OptIn(ExperimentalCoroutinesApi::class)
fun DefaultBehaviourContextWithFSM<State>.strictlyOnSendSolutionState(studentBotToken: String) {
  strictlyOn<SendSolutionState> { state ->
    val studentId = state.studentId
    val problem = state.problem

    var botMessage =
      bot.send(state.context, Dialogues.tellValidSolutionTypes(), replyMarkup = back())

    merge(
        waitDataCallbackQueryWithUser(state.context.id),
        waitTextMessageWithUser(state.context.id),
        waitMediaMessageWithUser(state.context.id),
        waitDocumentMessageWithUser(state.context.id),
      )
      .map { solutionMessage ->
        when (solutionMessage) {
          is DataCallbackQuery ->
            if (solutionMessage.data == RETURN_BACK) {
              deleteMessage(botMessage)
              MenuState(state.context, state.studentId)
            } else null

          is CommonMessage<*> -> {
            val attachment = extractSolutionContent(solutionMessage, studentBotToken)
            deleteMessage(botMessage)

            if (attachment == null) {
              botMessage =
                bot.send(state.context, Dialogues.tellSolutionTypeIsInvalid(), replyMarkup = back())
              return@map null
            }

            if (isDeadlineMissed(problem)) {
              bot.reply(solutionMessage, "К сожалению, дедлайн по задаче уже истек :(")
            } else {
              return@map ConfirmSubmissionState(
                state.context,
                Solution(
                  0L.toSolutionId(),
                  studentId,
                  solutionMessage.chat.id.chatId,
                  solutionMessage.messageId,
                  problem.id,
                  attachment,
                  null,
                  java.time.LocalDateTime.now().toKotlinLocalDateTime(),
                ),
              )
            }

            MenuState(state.context, state.studentId)
          }

          else -> null
        }
      }
      .filterNotNull()
      .first()
  }
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
