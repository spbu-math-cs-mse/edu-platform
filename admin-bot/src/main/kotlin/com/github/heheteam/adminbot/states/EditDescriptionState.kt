package com.github.heheteam.adminbot.states

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.state.BotState
import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.utils.newLine
import kotlinx.coroutines.flow.first

class EditDescriptionState(
  override val context: User,
  val course: Course,
  val courseName: String,
  val adminId: AdminId,
) : BotState<String, String?, Unit> {
  override suspend fun readUserInput(bot: BehaviourContext, service: Unit): String {
    bot.send(context) {
      +"Введите новое описание курса ${courseName}. Текущее описание:" + newLine + newLine
      +course.name
    }
    val message = bot.waitTextMessageWithUser(context.id).first()
    return message.content.text
  }

  override fun computeNewState(service: Unit, input: String): Pair<State, String?> {
    val response =
      when {
        input == "/stop" -> null
        else -> {
          //        course.name = answer TODO: implement this feature
          "Описание курса ${courseName} успешно обновлено"
        }
      }
    return Pair(MenuState(context, adminId), response)
  }

  override suspend fun sendResponse(bot: BehaviourContext, service: Unit, response: String?) {
    if (response != null) bot.send(context, response)
  }
}
