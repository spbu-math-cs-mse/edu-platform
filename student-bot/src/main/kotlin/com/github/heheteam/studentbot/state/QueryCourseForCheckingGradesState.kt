package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.util.BotStateWithHandlers
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.UpdateHandlersController
import com.github.heheteam.commonlib.util.UserInput
import com.github.heheteam.commonlib.util.createCoursePicker
import com.github.heheteam.commonlib.util.delete
import com.github.heheteam.studentbot.StudentApi
import com.github.michaelbull.result.mapBoth
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.bot.setMyCommands
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.BotCommand
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage

data class QueryCourseForCheckingGradesState(override val context: User, val studentId: StudentId) :
  BotStateWithHandlers<Course?, Unit, StudentApi> {
  private val sentMessages = mutableListOf<AccessibleMessage>()

  override suspend fun intro(
    bot: BehaviourContext,
    service: StudentApi,
    updateHandlersController: UpdateHandlersController<() -> Unit, Course?, Any>,
  ) {
    val courses = service.getStudentCourses(studentId)
    val coursesPicker = createCoursePicker(courses)
    val selectCourseMessage =
      bot.sendMessage(context.id, "Выберите курс", replyMarkup = coursesPicker.keyboard)
    sentMessages.add(selectCourseMessage)

    bot.setMyCommands(BotCommand("menu", "main menu"))
    updateHandlersController.addTextMessageHandler { maybeCommandMessage ->
      if (maybeCommandMessage.content.text == "/menu") {
        NewState(MenuState(context, studentId))
      } else {
        Unhandled
      }
    }
    updateHandlersController.addDataCallbackHandler { dataCallbackQuery ->
      coursesPicker
        .handler(dataCallbackQuery.data)
        .mapBoth(success = { UserInput(it) }, failure = { Unhandled })
    }
  }

  override fun computeNewState(service: StudentApi, input: Course?): Pair<State, Unit> =
    if (input != null) QueryAssignmentForCheckingGradesState(context, studentId, input.id) to Unit
    else {
      MenuState(context, studentId) to Unit
    }

  override suspend fun sendResponse(bot: BehaviourContext, service: StudentApi, response: Unit) {
    for (message in sentMessages) {
      bot.delete(message)
    }
  }

  override suspend fun outro(bot: BehaviourContext, service: StudentApi) = Unit
}
