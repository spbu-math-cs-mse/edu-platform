package com.github.heheteam.samplebot.state

import com.github.heheteam.samplebot.data.CoursesDistributor
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.types.chat.User

sealed interface BotState : State

data class MenuState(override val context: User) : BotState

data class ViewState(override val context: User) : BotState

data class SignUpState(override val context: User) : BotState {
  fun getAvailableCourses(coursesDistributor: CoursesDistributor) =
    coursesDistributor.getAvailableCourses(context.id.toString())

  val chosenCourses = mutableListOf<String>()
}
