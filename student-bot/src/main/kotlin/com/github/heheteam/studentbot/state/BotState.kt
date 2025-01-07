package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.api.StudentId
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.types.chat.User

sealed interface BotState : State

data class StartState(override val context: User) : BotState

data class DevStartState(override val context: User, val queryIdMessage: String? = null) : BotState

data class PresetStudentState(override val context: User, val studentId: StudentId) : BotState

data class MenuState(override val context: User, val studentId: StudentId) : BotState

data class ViewState(override val context: User, val studentId: StudentId) : BotState

data class SignUpState(override val context: User, val studentId: StudentId) : BotState

data class SendSolutionState(
  override val context: User,
  val studentId: StudentId,
  var selectedCourse: Course? = null,
) : BotState

data class CheckGradesState(override val context: User, val studentId: StudentId) : BotState
