package com.github.heheteam.studentbot

import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.state.NavigationBotStateWithHandlers
import com.github.heheteam.commonlib.util.MenuKeyboardData
import com.github.heheteam.commonlib.util.buildColumnMenu
import com.github.heheteam.commonlib.util.ok
import com.github.heheteam.commonlib.util.simpleButtonData
import com.github.heheteam.studentbot.state.MenuState
import com.github.heheteam.studentbot.state.StudentStartState
import com.github.heheteam.studentbot.state.quiz.L0Student
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.textsources.TextSourcesList
import dev.inmo.tgbotapi.utils.buildEntities

class ConfirmAndGoToQuestState(
  override val context: User,
  val firstName: String,
  val lastName: String,
  val grade: Int? = null,
) : NavigationBotStateWithHandlers<StudentApi>() {
  lateinit var id: StudentId
  override val introMessageContent: TextSourcesList = buildEntities {
    +"Отлично, $firstName! Ты записан(а) как $firstName $lastName!"
  }

  override fun createKeyboard(service: StudentApi): MenuKeyboardData<State?> {
    TODO("Not yet implemented")
  }

  override fun createIntroMessageContent(
    service: StudentApi
  ): Result<TextSourcesList, FrontendError> = binding {
    val studentId = service.createStudent(firstName, lastName, context.id.chatId.long).bind()
    id = studentId
    buildEntities {
      +"Отлично, $firstName! Ты записан(а) как $firstName $lastName!\n\n"
      +"Готов(а) к путешествию по Матемаланду? Там нас ждёт столько всего интересного!"
    }
  }

  override fun createKeyboardOrResult(
    service: StudentApi
  ): Result<MenuKeyboardData<State?>, FrontendError> =
    buildColumnMenu(
        simpleButtonData("Да, начинаем!") { L0Student(context, id) },
        simpleButtonData("Меню") { MenuState(context, id) },
      )
      .ok()

  override fun menuState(): State = this

  override fun defaultState(): State = StudentStartState(context, null)
}
