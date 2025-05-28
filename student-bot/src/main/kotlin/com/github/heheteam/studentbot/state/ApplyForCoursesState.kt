package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.state.BotStateWithHandlersAndStudentId
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.UpdateHandlersController
import com.github.heheteam.commonlib.util.delete
import com.github.heheteam.studentbot.Keyboards
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.error
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.bot.exceptions.CommonRequestException
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage

data class ApplyForCoursesState(override val context: User, override val userId: StudentId) :
  BotStateWithHandlersAndStudentId<Unit, Unit, StudentApi> {
  private val sentMessages = mutableListOf<AccessibleMessage>()

  override suspend fun intro(
    bot: BehaviourContext,
    service: StudentApi,
    updateHandlersController: UpdateHandlersController<() -> Unit, Unit, Any>,
  ) {
    val studentCourses = service.getStudentCourses(userId).toSet()
    val allCourses = service.getAllCourses().map { it to studentCourses.contains(it) }
    val selectCourseMessage =
      bot.sendMessage(
        context.id,
        "Список доступных курсов:",
        replyMarkup = Keyboards.coursesSelector(allCourses),
      )
    sentMessages.add(selectCourseMessage)
    updateHandlersController.addDataCallbackHandler { dataCallbackQuery ->
      if (dataCallbackQuery.data == Keyboards.RETURN_BACK) {
        NewState(MenuState(context, userId))
      } else {
        Unhandled
      }
    }
  }

  override fun computeNewState(service: StudentApi, input: Unit): Pair<State, Unit> =
    MenuState(context, userId) to Unit

  override suspend fun sendResponse(bot: BehaviourContext, service: StudentApi, response: Unit) {
    sentMessages.forEach { message ->
      try {
        bot.delete(message)
      } catch (e: CommonRequestException) {
        KSLog.error(e.message.toString())
      }
    }
  }

  override suspend fun outro(bot: BehaviourContext, service: StudentApi) = Unit
}
