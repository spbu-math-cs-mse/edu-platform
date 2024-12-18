package com.github.heheteam.teacherbot.states

import com.github.heheteam.commonlib.api.TeacherId
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.types.chat.User

sealed interface BotState : State

internal data class StartState(
    override val context: User,
) : BotState

internal data class MenuState(
    override val context: User,
    val teacherId: TeacherId,
) : BotState

internal data class PresetTeacherState(
    override val context: User,
    val teacherId: TeacherId,
) : BotState

internal data class GettingSolutionState(
    override val context: User,
    val teacherId: TeacherId,
) : BotState

internal data class CheckGradesState(
    override val context: User,
    val teacherId: TeacherId,
) : BotState
