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
import com.github.heheteam.teacherbot.states.SimpleTeacherState
import com.github.michaelbull.result.mapBoth
import dev.inmo.tgbotapi.types.chat.User

data class SelectCourseForQuiz(override val context: User, override val userId: TeacherId) :
  SimpleTeacherState() {
  override suspend fun BotContext.run(service: TeacherApi) {
    val courses = service.getTeacherCoursesForQuiz(userId).value
    val menu =
      buildColumnMenu(courses.map { simpleButtonData(it.name) { it } }).map {
        NewState(InputQuestionForQuiz(context, userId, QuizMetaInformationBuilder(it, userId)))
      }
    send("Выберите курс, для которого вы хотите составить запрос", replyMarkup = menu.keyboard)
    addDataCallbackHandler {
      menu.handler(it.data).mapBoth(success = ::id, failure = { Unhandled })
    }
  }
}
