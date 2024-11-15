package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.SolutionContent
import com.github.heheteam.commonlib.SolutionType
import com.github.heheteam.studentbot.StudentCore
import com.github.heheteam.studentbot.metaData.*
import dev.inmo.tgbotapi.extensions.api.deleteMessage
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
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.queries.callback.CallbackQuery
import dev.inmo.tgbotapi.utils.RiskFeature
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOf

@OptIn(RiskFeature::class, ExperimentalCoroutinesApi::class)
fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnSendSolutionState(core: StudentCore) {
  strictlyOn<SendSolutionState> { state ->
    val studentId = core.getUserId(state.context.id)!!
    val courses =
      core
        .getAvailableCourses(studentId)
        .filter { !it.second }

    if (courses.isEmpty()) {
      val message =
        bot.send(
          state.context,
          "Сначала запишитесь на курсы!",
          replyMarkup = back(),
        )
      waitDataCallbackQuery().first()
      deleteMessage(state.context.id, message.messageId)
      return@strictlyOn MenuState(state.context)
    }

    var courseMessage =
      bot.send(
        state.context,
        "Выберите курс для отправки решения:",
        replyMarkup = buildCoursesSendingSelector(courses.toMutableList()),
      )

    val callback = waitDataCallbackQuery().first()
    if (callback.data == ButtonKey.BACK) {
      deleteMessage(state.context.id, courseMessage.messageId)
      return@strictlyOn MenuState(state.context)
    }

    val courseId = callback.data.split(" ").last()
    state.selectedCourse = courses.first { it.first.id == courseId }.first
    deleteMessage(state.context.id, courseMessage.messageId)

    var typeSelectorMessage = bot.send(
      state.context,
      "Как бы вы хотели отправить решение?",
      replyMarkup = buildSendSolutionSelector(),
    )

    val type = waitDataCallbackQuery().first().data
    var lastMessage: ContentMessage<TextContent>? = null

    while (true) {
      if (type == ButtonKey.BACK) {
        deleteMessage(state.context.id, typeSelectorMessage.messageId)
        courseMessage =
          bot.send(
            state.context,
            courseMessage.text.toString(),
            replyMarkup = courseMessage.reply_markup,
          )
        continue
      } else {
        val promptMessage: ContentMessage<TextContent>
        deleteMessage(state.context.id, typeSelectorMessage.messageId)
        when (type) {
          "PHOTOS" -> {
            promptMessage =
              bot.send(
                state.context,
                "Отправь мне фото я отошлю решение на проверку!",
                replyMarkup = back(),
              )
          }

          "TEXT" -> {
            promptMessage =
              bot.send(
                state.context,
                "Напиши решение текстом и я отошлю его на проверку!",
                replyMarkup = back(),
              )
          }

          "DOCUMENT" -> {
            promptMessage =
              bot.send(
                state.context,
                "Отправь файл и я отошлю его на проверку!",
                replyMarkup = back(),
              )
          }

          else -> {
            break
          }
        }

        val content = flowOf(
          waitDataCallbackQuery(),
          waitTextMessage(),
          waitMediaMessage(),
          waitDocumentMessage(),
        )
          .flattenMerge().first()

        if (content is CallbackQuery) {
          typeSelectorMessage = bot.send(
            state.context,
            typeSelectorMessage.text.toString(),
            replyMarkup = typeSelectorMessage.reply_markup,
          )
          continue
        }

        val solutionContent: SolutionContent
        val messageId: MessageId

        if (content is CommonMessage<*>) {
          messageId = content.messageId

          val textSolution = content.content.textContentOrNull()
          val photoSolution = content.content.photoContentOrNull()
          val photosSolution = content.content.mediaGroupContentOrNull()?.group?.map { it.content.photoContentOrNull() }
          val documentSolution = content.content.documentContentOrNull()

          solutionContent = if (textSolution != null) {
            SolutionContent(text = textSolution.text)
          } else if (photoSolution != null) {
            SolutionContent(text = SolutionType.PHOTO.toString(), fileIds = listOf(photoSolution.media.fileId.fileId))
          } else if (photosSolution != null) {
            SolutionContent(text = SolutionType.PHOTOS.toString(), fileIds = photosSolution.map { it!!.media.fileId.fileId })
          } else if (documentSolution != null) {
            SolutionContent(text = SolutionType.DOCUMENT.toString(), fileIds = listOf(documentSolution!!.media.fileId.fileId))
          } else {
            break
          }

          core.inputSolution(studentId, state.context.id.chatId, messageId, solutionContent)

          deleteMessage(state.context.id, promptMessage.messageId)

          lastMessage =
            bot.send(
              state.context,
              "Решение отправлено на проверку!",
              replyMarkup = back(),
            )

          waitDataCallbackQuery().first()
          break
        }
      }
    }

    if (lastMessage != null) {
      deleteMessage(state.context.id, lastMessage.messageId)
    }

    MenuState(state.context)
  }
}
