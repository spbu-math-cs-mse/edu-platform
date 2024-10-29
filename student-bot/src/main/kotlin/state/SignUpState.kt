package com.github.heheteam.samplebot.state

import com.github.heheteam.samplebot.data.CoursesDistributor
import com.github.heheteam.samplebot.metaData.*
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.edit.reply_markup.editMessageReplyMarkup
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnSignUpState(coursesDistributor: CoursesDistributor) {
  strictlyOn<SignUpState> { state ->
    val studentId = state.context.id
    val availableCourses = state.getAvailableCourses(coursesDistributor)

    val initialMessage = bot.send(
      state.context,
      text = "Вот доступные курсы",
      replyMarkup = buildCoursesSelector(state.getAvailableCourses(coursesDistributor)),
    )

    while (true) {
      val callbackData = waitDataCallbackQuery().first().data

      when {
        callbackData.contains(ButtonKey.COURSE_ID) -> {
          val courseId = callbackData.split(" ").last()
          state.chosenCourses.add(courseId)
          availableCourses.removeIf { it.id == courseId }

          bot.editMessageReplyMarkup(
            state.context.id,
            initialMessage.messageId,
            replyMarkup = buildCoursesSelector(availableCourses),
          )
        }

        callbackData == ButtonKey.BACK -> {
          deleteMessage(state.context.id, initialMessage.messageId)
          break
        }

        callbackData == ButtonKey.APPLY -> {
          if (state.chosenCourses.isEmpty()) {
            deleteMessage(state.context.id, initialMessage.messageId)

            val lastMessage = bot.send(
              state.context,
              text = "Вы не выбрали ни одного курса!",
              replyMarkup = back(),
            )

            waitDataCallbackQuery().first()
            deleteMessage(state.context.id, lastMessage.messageId)

            return@strictlyOn SignUpState(state.context)
          } else {
            state.chosenCourses.forEach { coursesDistributor.addRecord(studentId.toString(), it) }

            deleteMessage(state.context.id, initialMessage.messageId)

            val lastMessage = bot.send(
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
