package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.AdminCore
import com.github.heheteam.adminbot.Dialogues.askCourse
import com.github.heheteam.adminbot.Dialogues.noCoursesWasFound
import com.github.heheteam.adminbot.Keyboards.buildCoursesSelector
import com.github.heheteam.adminbot.Keyboards.returnBack
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.util.waitDataCallbackQueryWithUser
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnGetProblemsState(core: AdminCore) {
    strictlyOn<GetProblemsState> { state ->
        val courses = core.getCourses().values.toList()
        if (courses.isEmpty()) {
            bot.send(
                state.context,
                text = noCoursesWasFound(),
            )
            return@strictlyOn MenuState(state.context)
        }

        val course = queryCourse(state, courses) ?: return@strictlyOn MenuState(state.context)

        val problems = core.getProblemsBulletList(course)
        val problemsMessage = bot.send(
            state.context,
            text = problems,
            replyMarkup = returnBack(),
        )

        waitDataCallbackQueryWithUser(state.context.id).first()
        deleteMessage(problemsMessage)
        MenuState(state.context)
    }
}

private suspend fun BehaviourContext.queryCourse(
    state: GetProblemsState,
    courses: List<Course>,
): Course? {
    val message =
        bot.send(state.context, askCourse(), replyMarkup = buildCoursesSelector(courses))

    val callbackData = waitDataCallbackQueryWithUser(state.context.id).first().data
    deleteMessage(message)

    if (callbackData == returnBack) {
        return null
    }

    val courseId = callbackData.split(" ").last()
    return courses.first { it.id == CourseId(courseId.toLong()) }
}
