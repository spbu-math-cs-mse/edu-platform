package com.github.heheteam.teacherbot.states

import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.toCourseId
import com.github.heheteam.commonlib.util.BotState
import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.Chat
import dev.inmo.tgbotapi.types.toChatId
import kotlinx.coroutines.flow.first

class ChooseGroupCourseState(override val context: Chat) : BotState<CourseId?, CourseId?, Unit> {
  override suspend fun readUserInput(bot: BehaviourContext, service: Unit): CourseId? {
    with(bot) {
      sendMessage(context, "Введите id курса")
      val idText = waitTextMessageWithUser(context.id.toChatId()).first().content.text
      return idText.toLongOrNull()?.toCourseId()
    }
  }

  override fun computeNewState(service: Unit, input: CourseId?): Pair<State, CourseId?> =
    if (input != null) {
      ListeningForSolutionsGroupState(context, input)
    } else {
      ChooseGroupCourseState(context)
    } to input

  override suspend fun sendResponse(bot: BehaviourContext, service: Unit, response: CourseId?) {
    with(bot) {
      if (response == null) {
        sendMessage(context, "Bad request")
      }
    }
  }
}
