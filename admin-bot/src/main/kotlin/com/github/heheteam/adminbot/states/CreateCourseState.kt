package com.github.heheteam.adminbot.states

import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.state.BotStateWithHandlers
import com.github.heheteam.commonlib.util.UpdateHandlersController
import com.github.heheteam.commonlib.util.delete
import com.github.heheteam.commonlib.util.toUrl
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.get
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage

class CreateCourseState(override val context: User) :
  BotStateWithHandlers<String, String?, AdminApi> {

  val sentMessages = mutableListOf<AccessibleMessage>()

  override suspend fun outro(bot: BehaviourContext, service: AdminApi) {
    sentMessages.forEach { bot.delete(it) }
  }

  override suspend fun intro(
    bot: BehaviourContext,
    service: AdminApi,
    updateHandlersController: UpdateHandlersController<BehaviourContext.() -> Unit, String, Any>,
  ) {
    val introMessage =
      bot.send(
        context,
        "Введите название курса, который хотите создать, или отправьте /stop, чтобы отменить операцию",
      )
    sentMessages.add(introMessage)
  }

  override fun computeNewState(service: AdminApi, input: String): Pair<State, String?> {
    val response =
      when {
        input == "/stop" -> null
        service.getCourses().map { it.value }.any { it.name == input } ->
          "Курс с таким названием уже существует"
        else -> {
          val courseId = service.createCourse(input)
          binding {
              val (_, spreadsheetId) = service.resolveCourseWithSpreadsheetId(courseId).bind()
              "Курс $input успешно создан\nРейтинг доступен по ссылке:\n\n${spreadsheetId.toUrl()}"
            }
            .get() ?: "Не удалось создать курс $input\n"
        }
      }
    return Pair(MenuState(context), response)
  }

  override suspend fun sendResponse(bot: BehaviourContext, service: AdminApi, response: String?) {
    if (response != null) {
      bot.send(context, response)
    }
  }
}
