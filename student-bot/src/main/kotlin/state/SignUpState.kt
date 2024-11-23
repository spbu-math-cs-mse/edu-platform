package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.UserIdRegistry
import com.github.heheteam.studentbot.StudentCore
import com.github.heheteam.studentbot.metaData.*
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.edit.reply_markup.editMessageReplyMarkup
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.reply_markup
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.text
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.utils.RiskFeature
import kotlinx.coroutines.flow.first

@OptIn(RiskFeature::class)
fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnSignUpState(
  userIdRegistry: UserIdRegistry,
  core: StudentCore,
) {
  strictlyOn<SignUpState> { state ->
    val studentId = userIdRegistry.getUserId(state.context.id)!!
    val availableCourses = core.getAvailableCourses(studentId).toMutableList()

    val initialMessage =
      bot.send(
        state.context,
        text = "Вот доступные курсы",
        replyMarkup = buildCoursesSelector(availableCourses),
      )

    val signingUpState = SigningUpState(state, availableCourses, core, studentId)
    signingUpState.run {
      signUp(initialMessage)
    }

    MenuState(state.context)
  }
}

class SigningUpState(
  private val state: SignUpState,
  private val availableCourses: MutableList<Pair<Course, Boolean>>,
  private val core: StudentCore,
  private val studentId: String,
) {

  suspend fun BehaviourContext.signUp(initialMessage: ContentMessage<*>) {
    courseIndex(initialMessage)
  }

  private suspend fun BehaviourContext.courseIndex(
    message: ContentMessage<*>,
  ) {
    val callbackData = waitDataCallbackQuery().first().data

    if (callbackData == ButtonKey.APPLY) {
      return applyWithCourses(message)
    }

    val courseId = callbackData.split(" ").last()

    val index = when {
      callbackData.contains(ButtonKey.COURSE_ID) -> {
        val courseId = callbackData.split(" ").last()
        availableCourses.indexOfFirst { it.first.id == courseId }
      }

      else -> {
        deleteMessage(message)
        null
      }
    }

    return courseByIndex(message, index, courseId)
  }

  @OptIn(RiskFeature::class)
  private suspend fun BehaviourContext.courseByIndex(
    message: ContentMessage<*>,
    index: Int?,
    courseId: String,
  ) {
    if (index == null) {
      return
    }

    if (!availableCourses[index].second) {
      deleteMessage(message)

      val botMessage = bot.send(
        state.context,
        text = "Вы уже выбрали этот курс или записаны на него!",
        replyMarkup = back(),
      )

      waitDataCallbackQuery().first()
      deleteMessage(botMessage)

      val newMessage = bot.send(
        state.context,
        text = message.text.toString(),
        replyMarkup = buildCoursesSelector(availableCourses),
      )

      return courseIndex(newMessage)
    }

    state.chosenCourses.add(courseId)
    availableCourses[index] = Pair(availableCourses[index].first, false)

    bot.editMessageReplyMarkup(
      state.context.id,
      message.messageId,
      replyMarkup = buildCoursesSelector(availableCourses),
    )

    return courseIndex(message)
  }

  @OptIn(RiskFeature::class)
  private suspend fun BehaviourContext.applyWithCourses(
    message: ContentMessage<*>,
  ) {
    when {
      state.chosenCourses.isEmpty() -> {
        deleteMessage(message)

        val botMessage = bot.send(
          state.context,
          text = "Вы не выбрали ни одного курса!",
          replyMarkup = back(),
        )

        waitDataCallbackQuery().first()
        deleteMessage(botMessage)

        val newMessage = bot.send(
          state.context,
          text = message.text.toString(),
          replyMarkup = message.reply_markup,
        )

        return courseIndex(newMessage)
      }

      else -> {
        state.chosenCourses.forEach { courseId -> core.addRecord(studentId, courseId) }

        deleteMessage(message)

        val botMessage = bot.send(
          state.context,
          text = "Вы успешно записались на курсы!",
          replyMarkup = back(),
        )

        waitDataCallbackQuery().first()
        deleteMessage(botMessage)

        return
      }
    }
  }
}
