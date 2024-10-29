package com.github.heheteam.samplebot.state

import com.github.heheteam.samplebot.data.CoursesDistributor
import com.github.heheteam.samplebot.metaData.back
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnViewState(coursesDistributor: CoursesDistributor) {
  strictlyOn<ViewState> { state ->
    val studentId = state.context.id
    val studentCourses = coursesDistributor.getCourses(studentId.toString())

    val initialMessage = bot.send(
      state.context,
      text = studentCourses,
      replyMarkup = back(),
    )

    waitDataCallbackQuery().first()
    deleteMessage(state.context.id, initialMessage.messageId)

    MenuState(state.context)
  }
}
