package com.github.heheteam.parentbot.states

import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.api.ParentId
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.types.chat.User

sealed interface BotState : State

internal data class StartState(
    override val context: User,
) : BotState

internal data class MenuState(
    override val context: User,
    val parentId: ParentId,
) : BotState

internal data class ChildPerformanceState(
    override val context: User,
    val child: Student,
    val parentId: ParentId,
) : BotState

internal data class GivingFeedbackState(
    override val context: User,
    val parentId: ParentId,
) : BotState
