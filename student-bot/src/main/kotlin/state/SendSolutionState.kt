package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.SolutionContent
import com.github.heheteam.commonlib.SolutionType
import com.github.heheteam.studentbot.Dialogues
import com.github.heheteam.studentbot.StudentCore
import com.github.heheteam.studentbot.metaData.ButtonKey
import com.github.heheteam.studentbot.metaData.back
import com.github.heheteam.studentbot.metaData.buildCoursesSendingSelector
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.edit.reply_markup.editMessageReplyMarkup
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDocumentMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitMediaMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import dev.inmo.tgbotapi.extensions.utils.documentContentOrNull
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.reply_markup
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.text
import dev.inmo.tgbotapi.extensions.utils.mediaGroupContentOrNull
import dev.inmo.tgbotapi.extensions.utils.photoContentOrNull
import dev.inmo.tgbotapi.extensions.utils.textContentOrNull
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.queries.callback.CallbackQuery
import dev.inmo.tgbotapi.utils.RiskFeature
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOf

@OptIn(RiskFeature::class, ExperimentalCoroutinesApi::class)
fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnSendSolutionState(core: StudentCore) {
  strictlyOn<SendSolutionState> { state ->
    val studentId = core.userIdRegistry.getUserId(state.context.id)!!
    val courses =
      core.coursesDistributor
        .getAvailableCourses(studentId)
        .filter { !it.second }

    if (courses.isEmpty()) {
      val message =
        bot.send(
          state.context,
          Dialogues.tellToApplyForCourses(),
          replyMarkup = back(),
        )
      waitDataCallbackQuery().first()
      deleteMessage(state.context.id, message.messageId)
      return@strictlyOn MenuState(state.context)
    }

    var initialMessage =
      bot.send(
        state.context,
        Dialogues.askCourseForSolution(),
        replyMarkup = buildCoursesSendingSelector(courses.toMutableList()),
      )

    while (true) {
      val callbackData = waitDataCallbackQuery().first().data

      if (callbackData == ButtonKey.BACK) {
        deleteMessage(state.context.id, initialMessage.messageId)
        break
      }

      if (callbackData.contains(ButtonKey.COURSE_ID)) {
        val courseId = callbackData.split(" ").last()

        state.selectedCourse = courses.first { it.first.id == courseId }.first

        deleteMessage(state.context.id, initialMessage.messageId)

        val selectSolutionTypePrompt =
          bot.send(
            state.context,
            Dialogues.tellValidSolutionTypes(),
            replyMarkup = back(),
          )

        val content =
          flowOf(
            waitDataCallbackQuery(),
            waitTextMessage(),
            waitMediaMessage(),
            waitDocumentMessage(),
          ).flattenMerge()
            .first()

        if (content is CallbackQuery) {
          deleteMessage(state.context.id, selectSolutionTypePrompt.messageId)
          initialMessage =
            bot.send(
              state.context,
              initialMessage.text.toString(),
              replyMarkup = initialMessage.reply_markup,
            )
          continue
        }

        initialMessage = selectSolutionTypePrompt

        if (content is CommonMessage<*>) {
          val messageId = content.messageId

          val textSolution = content.content.textContentOrNull()
          val photoSolution = content.content.photoContentOrNull()
          val photosSolution =
            content.content.mediaGroupContentOrNull()?.group?.mapNotNull {
              it.content.photoContentOrNull()
                ?: it.content.documentContentOrNull()
            }
          val documentSolution = content.content.documentContentOrNull()

          val solutionContent =
            if (textSolution != null) {
              SolutionContent(text = textSolution.text)
            } else if (photoSolution != null) {
              SolutionContent(
                text = SolutionType.PHOTO.toString(),
                fileIds = listOf(photoSolution.media.fileId.fileId)
              )
            } else if (photosSolution != null) {
              SolutionContent(
                text = SolutionType.PHOTOS.toString(),
                fileIds = photosSolution.map { it!!.media.fileId.fileId })
            } else if (documentSolution != null) {
              SolutionContent(
                text = SolutionType.DOCUMENT.toString(),
                fileIds = listOf(documentSolution.media.fileId.fileId)
              )
            } else {
              deleteMessage(state.context.id, initialMessage.messageId)
              val invalidSolutionTypePrompt =
                bot.send(
                  state.context,
                  Dialogues.tellSolutionTypeIsInvalid(),
                  replyMarkup = back(),
                )
              deleteMessage(
                state.context.id,
                invalidSolutionTypePrompt.messageId
              )
              waitDataCallbackQuery().first()
              initialMessage =
                bot.send(
                  state.context,
                  initialMessage.text.toString(),
                  replyMarkup = initialMessage.reply_markup,
                )
              continue
            }

          core.solutionDistributor.inputSolution(
            studentId,
            state.context.id.chatId,
            messageId,
            solutionContent
          )

          deleteMessage(state.context.id, initialMessage.messageId)

          initialMessage =
            bot.send(
              state.context,
              Dialogues.tellSolutionIsSent(),
              replyMarkup = back(),
            )

          waitDataCallbackQuery().first()

          bot.editMessageReplyMarkup(
            initialMessage,
            replyMarkup = null,
          )

          break
        }
      }
    }

    MenuState(state.context)
  }
}
