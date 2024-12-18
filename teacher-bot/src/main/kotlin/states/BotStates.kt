package com.github.heheteam.teacherbot.states

import com.github.heheteam.commonlib.api.TeacherId
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.types.chat.User

sealed interface BotState : State

data class StartState(
    override val context: User,
) : BotState

data class MenuState(
    override val context: User,
    val teacherId: TeacherId,
) : BotState

data class PresetTeacherState(
    override val context: User,
    val teacherId: TeacherId,
) : BotState

data class GettingSolutionState(
    override val context: User,
    val teacherId: TeacherId,
) : BotState

data class CheckGradesState(
    override val context: User,
    val teacherId: TeacherId,
) : BotState
