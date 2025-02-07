package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.Keyboards.returnBack
import com.github.heheteam.commonlib.api.TeacherStorage
import com.github.heheteam.commonlib.util.BotState
import com.github.heheteam.commonlib.util.waitDataCallbackQueryWithUser
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import kotlinx.coroutines.flow.first

class GetTeachersState(override val context: User) : BotState<Unit, Unit, TeacherStorage> {
  override suspend fun readUserInput(bot: BehaviourContext, service: TeacherStorage) {
    val teachers = getTeachersBulletList(service)
    val teachersMessage = bot.send(context, text = teachers, replyMarkup = returnBack())
    bot.waitDataCallbackQueryWithUser(context.id).first()
    bot.deleteMessage(teachersMessage)
  }

  override fun computeNewState(service: TeacherStorage, input: Unit): Pair<State, Unit> {
    return Pair(MenuState(context), Unit)
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: TeacherStorage,
    response: Unit,
  ) = Unit

  private fun getTeachersBulletList(teacherStorage: TeacherStorage): String {
    val teachersList = teacherStorage.getTeachers()
    val noTeachers = "Список преподавателей пуст!"
    return if (teachersList.isNotEmpty()) {
      teachersList
        .sortedBy { it.surname }
        .joinToString("\n") { teacher -> "- ${teacher.surname} ${teacher.name}" }
    } else {
      noTeachers
    }
  }
}
