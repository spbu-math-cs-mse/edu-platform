package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.UserIdRegistry
import com.github.heheteam.studentbot.StudentCore
import com.github.heheteam.studentbot.metaData.*
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.edit.reply_markup.editMessageReplyMarkup
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.text
import dev.inmo.tgbotapi.utils.RiskFeature
import kotlinx.coroutines.flow.first

@OptIn(RiskFeature::class)
fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnSignUpState(
  userIdRegistry: UserIdRegistry,
  core: StudentCore,
) {
  strictlyOn<SignUpState> { state ->
    val studentId = userIdRegistry.getUserId(state.context.id)!!
    val courses = core.getCourses()
    val availableCourses = core.getAvailableCourses(studentId).toMutableList()
    val coursesToAvailability = courses.map { it to availableCourses.contains(it) }.toMutableList()

    var initialMessage =
      bot.send(
        state.context,
        text = "Вот доступные курсы",
        replyMarkup = buildCoursesSelector(coursesToAvailability),
      )

    while (true) {
      val callbackData = waitDataCallbackQuery().first().data

      when {
        callbackData.contains(ButtonKey.COURSE_ID) -> {
          val courseId = callbackData.split(" ").last()

          val index = courses.indexOfFirst { it.id == courseId }

          if (availableCourses.contains(courses[index])) {
            deleteMessage(state.context.id, initialMessage.messageId)

            val lastMessage =
              bot.send(
                state.context,
                text = "Вы уже записаны на этот курс!",
                replyMarkup = back(),
              )

            waitDataCallbackQuery().first()

            deleteMessage(state.context.id, lastMessage.messageId)

            initialMessage =
              bot.send(
                state.context.id,
                text = initialMessage.text.toString(),
                replyMarkup = buildCoursesSelector(coursesToAvailability),
              )
            continue
          }

          availableCourses.add(courses[index])
          coursesToAvailability[coursesToAvailability.indexOfFirst { it.first.id == courseId }]=courses[index] to true

          bot.editMessageReplyMarkup(
            state.context.id,
            initialMessage.messageId,
            replyMarkup = buildCoursesSelector(coursesToAvailability),
          )
        }

        callbackData == ButtonKey.BACK -> {
          deleteMessage(state.context.id, initialMessage.messageId)
          break
        }

        callbackData == ButtonKey.APPLY -> {
          if (availableCourses.isEmpty()) {
            deleteMessage(state.context.id, initialMessage.messageId)

            val lastMessage =
              bot.send(
                state.context,
                text = "Вы не выбрали ни одного курса!",
                replyMarkup = back(),
              )

            waitDataCallbackQuery().first()
            deleteMessage(state.context.id, lastMessage.messageId)

            return@strictlyOn SignUpState(state.context)
          } else {
            availableCourses.forEach { core.addRecord(studentId, it.id) }

            deleteMessage(state.context.id, initialMessage.messageId)

            val lastMessage =
              bot.send(
                state.context,
                text = "Вы успешно записались на курсы!",
                replyMarkup = back(),
              )

            waitDataCallbackQuery().first()
            deleteMessage(state.context.id, lastMessage.messageId)

            break
          }
        }

        else -> {
          break
        }
      }
    }
    MenuState(state.context)
  }
}
