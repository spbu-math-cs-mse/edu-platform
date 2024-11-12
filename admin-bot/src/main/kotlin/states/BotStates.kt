package com.github.heheteam.adminbot.states

import Course
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.types.chat.User
import java.time.LocalDate

sealed interface BotState : State

data class StartState(
    override val context: User,
) : BotState

data class NotAdminState(
    override val context: User,
) : BotState

data class MenuState(
    override val context: User,
) : BotState

data class CreateCourseState(
    override val context: User,
) : BotState

data class PickACourseState(
    override val context: User,
) : BotState

data class EditCourseState(
    override val context: User,
    val course: Course,
    val courseName: String,
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

data class ScheduleMessageSelectDateState(
    override val context: User,
    val course: Course,
    val courseName: String,
    val text: String,
) : BotState

data class ScheduleMessageEnterDateState(
    override val context: User,
    val course: Course,
    val courseName: String,
    val text: String,
) : BotState

data class ScheduleMessageEnterTimeState(
    override val context: User,
    val course: Course,
    val courseName: String,
    val text: String,
    val date: LocalDate
) : BotState