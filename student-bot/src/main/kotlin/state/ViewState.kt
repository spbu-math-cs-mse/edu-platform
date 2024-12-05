package com.github.heheteam.studentbot.state

import com.github.heheteam.studentbot.StudentCore
import com.github.heheteam.studentbot.metaData.back
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnViewState(
  core: StudentCore,
) {
  strictlyOn<ViewState> { state ->
    val studentId = state.studentId
    val studentCourses = core.getCoursesBulletList(studentId)
    val initialMessage =
      bot.send(
        state.context,
        text = studentCourses,
        replyMarkup = back(),
      )
    waitDataCallbackQuery().first()
    deleteMessage(initialMessage)
    MenuState(state.context, state.studentId)
  }
}
