package com.github.heheteam.adminbot.states

import com.github.heheteam.commonlib.Course
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.types.chat.User

sealed interface BotState : State

internal data class StartState(
    override val context: User,
) : BotState

internal data class MenuState(
    override val context: User,
) : BotState

internal data class CreateCourseState(
    override val context: User,
) : BotState

internal data class GetTeachersState(
    override val context: User,
) : BotState

internal data class GetProblemsState(
    override val context: User,
) : BotState

internal data class EditCourseState(
    override val context: User,
) : BotState

internal data class AddStudentState(
    override val context: User,
    val course: Course,
    val courseName: String,
) : BotState

internal data class RemoveStudentState(
    override val context: User,
    val course: Course,
    val courseName: String,
) : BotState

internal data class AddTeacherState(
    override val context: User,
    val course: Course,
    val courseName: String,
) : BotState

internal data class RemoveTeacherState(
    override val context: User,
    val course: Course,
    val courseName: String,
) : BotState

internal data class EditDescriptionState(
    override val context: User,
    val course: Course,
    val courseName: String,
) : BotState

internal data class AddScheduledMessageState(
    override val context: User,
    val course: Course,
    val courseName: String,
) : BotState
