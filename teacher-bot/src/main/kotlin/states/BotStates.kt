package com.github.heheteam.teacherbot.states

import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.types.chat.User

sealed interface BotState : State

data class StartState(
  override val context: User,
) : BotState

data class MenuState(
  override val context: User,
) : BotState

data class GettingSolutionState(
  override val context: User,
) : BotState

data class TestSendingSolutionState(
  override val context: User,
) : BotState

data class CheckGradesState(
  override val context: User,
) : BotState
