package com.github.heheteam.adminbot.states

import com.github.heheteam.commonlib.api.CoursesDistributor
import com.github.heheteam.commonlib.util.BotState
import com.github.heheteam.commonlib.util.toUrl
import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.get
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import kotlinx.coroutines.flow.first

class CreateCourseState(override val context: User) :
  BotState<String, String?, CoursesDistributor> {
  override suspend fun readUserInput(bot: BehaviourContext, service: CoursesDistributor): String {
    bot.send(
      context,
      "Введите название курса, который хотите создать, или отправьте /stop, чтобы отменить операцию",
    )

    val message = bot.waitTextMessageWithUser(context.id).first()
    return message.content.text
  }

  override fun computeNewState(service: CoursesDistributor, input: String): Pair<State, String?> {
    val response =
      when {
        input == "/stop" -> null

        service.getCourses().any { it.name == input } -> "Курс с таким названием уже существует"

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

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: CoursesDistributor,
    response: String?,
  ) {
    if (response != null) bot.send(context, response)
  }
}
