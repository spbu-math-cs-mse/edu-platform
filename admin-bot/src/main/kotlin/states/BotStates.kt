package com.github.heheteam.adminbot.states

import com.github.heheteam.commonlib.Course
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.types.chat.User

sealed interface BotState : State

data class StartState(
    override val context: User,
) : BotState

data class MenuState(
    override val context: User,
) : BotState

data class CreateCourseState(
    override val context: User,
) : BotState

data class GetTeachersState(
    override val context: User,
) : BotState

data class GetProblemsState(
    override val context: User,
) : BotState

data class EditCourseState(
    override val context: User,
) : BotState

data class AddStudentState(
    override val context: User,
    val course: Course,
    val courseName: String,
) : BotState

data class RemoveStudentState(
    override val context: User,
    val course: Course,
    val courseName: String,
) : BotState

data class AddTeacherState(
    override val context: User,
    val course: Course,
    val courseName: String,
) : BotState

data class RemoveTeacherState(
    override val context: User,
    val course: Course,
    val courseName: String,
) : BotState

data class EditDescriptionState(
    override val context: User,
    val course: Course,
    val courseName: String,
) : BotState

data class AddScheduledMessageState(
    override val context: User,
    val course: Course,
    val courseName: String,
) : BotState
