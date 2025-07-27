package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.api.ParentApi
import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.studentbot.state.parent.ParentMenuState
import com.github.michaelbull.result.binding
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.toChatId

class StartState(
  override val context: User,
  val from: String? = null,
  val courseToken: String? = null,
) : State {
  fun handle(studentApi: StudentApi, parentApi: ParentApi): State {
    val result = binding {
      val student = studentApi.loginByTgId(context.id).bind()
      if (student != null) {
        MenuState(context, student.id, courseToken = courseToken)
      } else {
        val parent = parentApi.tryLoginByTelegramId(context.id.toChatId().chatId).bind()
        if (parent != null) {
          ParentMenuState(context, parent.id)
        } else {
          SelectStudentParentState(context, from, courseToken)
        }
      }
    }
    return result.value
  }
}
