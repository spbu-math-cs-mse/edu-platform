package com.github.heheteam.teacherbot.states.quiz

import com.github.heheteam.commonlib.api.TeacherApi
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.heheteam.commonlib.state.BotContext
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.buildColumnMenu
import com.github.heheteam.commonlib.util.id
import com.github.heheteam.commonlib.util.map
import com.github.heheteam.commonlib.util.simpleButtonData
import com.github.heheteam.teacherbot.Keyboards
import com.github.heheteam.teacherbot.states.MenuState
import com.github.heheteam.teacherbot.states.SimpleTeacherState
import com.github.michaelbull.result.mapBoth
import dev.inmo.tgbotapi.types.chat.User

data class SelectCourseForQuiz(override val context: User, override val userId: TeacherId) :
  SimpleTeacherState() {
  override suspend fun BotContext.run(service: TeacherApi) {
    val courses = service.getTeacherCoursesForQuiz(userId).value
    if (courses.isEmpty()) {
      send("Вы не записаны ни на один курс", replyMarkup = Keyboards.back())
      addDataCallbackHandler { NewState(MenuState(context, userId)) }
      return
    }
    val menu =
      buildColumnMenu((courses + null).map { simpleButtonData(it?.name ?: "Назад") { it } }).map {
        course ->
        if (course != null) {
          NewState(
            InputQuestionForQuiz(context, userId, QuizMetaInformationBuilder(course, userId))
          )
        } else {
          NewState(MenuState(context, userId))
        }
      }
    send("Выберите курс, для которого вы хотите составить опрос", replyMarkup = menu.keyboard)
      .deleteLater()
    addDataCallbackHandler {
      menu.handler(it.data).mapBoth(success = ::id, failure = { Unhandled })
    }
  }
}
