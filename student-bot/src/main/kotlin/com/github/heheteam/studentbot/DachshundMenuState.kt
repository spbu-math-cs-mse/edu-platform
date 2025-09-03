package com.github.heheteam.studentbot

import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.state.BotContext
import com.github.heheteam.commonlib.state.SimpleState
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.studentbot.state.MenuState
import com.github.heheteam.studentbot.state.SolutionsStudentMenuState
import com.github.heheteam.studentbot.state.StudentAboutCourseState
import com.github.heheteam.studentbot.state.StudentKeyboards
import com.github.heheteam.studentbot.state.quiz.L0Student
import com.github.heheteam.studentbot.state.quiz.QuestState
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.get
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.utils.buildEntities

class DachshundMenuState(override val context: User, override val userId: StudentId) :
  SimpleState<StudentApi, StudentId>() {
  fun handleKeyboardCallback(data: String, service: StudentApi) = binding {
    when (data) {
      StudentKeyboards.ABOUT_COURSE -> StudentAboutCourseState(context, userId)
      StudentKeyboards.FREE_ACTIVITY -> {
        val stateName = service.resolveCurrentQuestState(userId).bind()
        val state = QuestState.restoreState<StudentApi, StudentId>(stateName, context, userId).get()
        state ?: L0Student(context, userId)
      }
      StudentKeyboards.SOLUTIONS -> SolutionsStudentMenuState(context, userId)
      StudentKeyboards.RETURN_BACK -> MenuState(context, userId)
      else -> null
    }
  }

  override suspend fun BotContext.run(service: StudentApi) {
    send(buildEntities { +"\uD83D\uDC36 Меню квеста от Таксы" }, StudentKeyboards.dachshundMenu())
      .deleteLater()
    addDataCallbackHandler { callback ->
      handleKeyboardCallback(callback.data, service).value?.let { NewState(it) } ?: Unhandled
    }
  }

  override fun defaultState(): State = MenuState(context, userId)
}
