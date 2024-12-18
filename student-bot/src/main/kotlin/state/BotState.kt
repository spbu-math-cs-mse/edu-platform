package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.api.StudentId
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.types.chat.User

sealed interface BotState : State

internal data class StartState(
    override val context: User,
) : BotState

internal data class DevStartState(
    override val context: User,
    val queryIdMessage: String? = null,
) : BotState

internal data class PresetStudentState(
    override val context: User,
    val studentId: StudentId,
) : BotState

internal data class MenuState(
    override val context: User,
    val studentId: StudentId,
) : BotState

internal data class ViewState(
    override val context: User,
    val studentId: StudentId,
) : BotState

data class SignUpState(
    override val context: User,
    val studentId: StudentId,
) : BotState

internal data class SendSolutionState(
    override val context: User,
    val studentId: StudentId,
    var selectedCourse: Course? = null,
) : BotState

internal data class CheckGradesState(
    override val context: User,
    val studentId: StudentId,
) : BotState
