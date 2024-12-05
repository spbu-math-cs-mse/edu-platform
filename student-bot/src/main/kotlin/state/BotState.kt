package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.Student
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.types.chat.User

sealed interface BotState : State

data class StartState(
  override val context: User,
) : BotState

data class MenuState(
  override val context: User,
  val student: Student,
) : BotState

data class ViewState(
  override val context: User,
  val student: Student,
) : BotState

data class SignUpState(
  override val context: User,
  val student: Student,
) : BotState

data class SendSolutionState(
  override val context: User,
  val student: Student,
  var selectedCourse: Course? = null,
) : BotState

data class CheckGradesState(
  override val context: User,
  val student: Student,
) : BotState
