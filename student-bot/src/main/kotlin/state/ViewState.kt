package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.api.StudentIdRegistry
import com.github.heheteam.studentbot.StudentCore
import com.github.heheteam.studentbot.metaData.back
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnViewState(
  userIdRegistry: StudentIdRegistry,
  core: StudentCore,
) {
  strictlyOn<ViewState> { state ->
    val studentId = userIdRegistry.getUserId(state.context.id).value
    val studentCourses = core.getCoursesBulletList(studentId)
    val initialMessage =
      bot.send(
        state.context,
        text = studentCourses,
        replyMarkup = back(),
      )
    waitDataCallbackQuery().first()
    deleteMessage(initialMessage)
    MenuState(state.context)
  }
}
