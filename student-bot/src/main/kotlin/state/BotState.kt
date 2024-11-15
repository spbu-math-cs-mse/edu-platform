package com.github.heheteam.studentbot.state

import Course
import com.github.heheteam.studentbot.StudentCore
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.types.chat.User

sealed interface BotState : State

data class MenuState(
  override val context: User,
) : BotState

data class ViewState(
  override val context: User,
) : BotState

data class SignUpState(
  override val context: User,
) : BotState {
  fun getAvailableCourses(core: StudentCore): MutableList<Pair<Course, Boolean>> =
    core.getAvailableCourses(context.id.toString())

  val chosenCourses = mutableListOf<String>()
}

data class CheckGradesState(
  override val context: User,
) : BotState
