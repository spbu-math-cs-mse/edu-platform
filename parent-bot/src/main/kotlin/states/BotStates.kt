package states

import Student
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.types.chat.User

sealed interface BotState : State

data class StartState(
  override val context: User,
) : BotState

data class MenuState(
  override val context: User,
) : BotState

data class ChildPerformanceState(
  override val context: User,
  val child: Student,
) : BotState

data class GivingFeedbackState(
  override val context: User,
) : BotState
