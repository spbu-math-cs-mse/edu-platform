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
    val availableCourses = core.getAvailableCourses(studentId).toMutableList()

    var initialMessage =
      bot.send(
        state.context,
        text = "Вот доступные курсы",
        replyMarkup = buildCoursesSelector(availableCourses),
      )

    while (true) {
      val callbackData = waitDataCallbackQuery().first().data

      when {
        callbackData.contains(ButtonKey.COURSE_ID) -> {
          val courseId = callbackData.split(" ").last()

          val index = availableCourses.indexOfFirst { it.first.id == courseId }

          if (!availableCourses[index].second) {
            deleteMessage(initialMessage)

            val lastMessage =
              bot.send(
                state.context,
                text = "Вы уже записаны на этот курс!",
                replyMarkup = back(),
              )

            waitDataCallbackQuery().first()

            deleteMessage(initialMessage)

            initialMessage =
              bot.send(
                state.context.id,
                text = initialMessage.text.toString(),
                replyMarkup = buildCoursesSelector(availableCourses),
              )
            continue
          }

          state.chosenCourses.add(courseId)

          availableCourses[index] = Pair(availableCourses[index].first, false)

          bot.editMessageReplyMarkup(
            state.context.id,
            initialMessage.messageId,
            replyMarkup = buildCoursesSelector(availableCourses),
          )
        }

        callbackData == ButtonKey.BACK -> {
          deleteMessage(initialMessage)
          break
        }

        callbackData == ButtonKey.APPLY -> {
          if (state.chosenCourses.isEmpty()) {
            deleteMessage(initialMessage)

            val lastMessage =
              bot.send(
                state.context,
                text = "Вы не выбрали ни одного курса!",
                replyMarkup = back(),
              )

            waitDataCallbackQuery().first()
            deleteMessage(initialMessage)

            return@strictlyOn SignUpState(state.context)
          } else {
            state.chosenCourses.forEach { courseId -> core.addRecord(studentId, courseId) }

            deleteMessage(initialMessage)

            val lastMessage =
              bot.send(
                state.context,
                text = "Вы успешно записались на курсы!",
                replyMarkup = back(),
              )

            waitDataCallbackQuery().first()
            deleteMessage(initialMessage)

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
