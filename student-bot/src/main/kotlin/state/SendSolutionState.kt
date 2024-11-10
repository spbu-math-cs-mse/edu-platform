package com.github.heheteam.studentbot.state

import com.github.heheteam.studentbot.data.CoursesDistributor
import com.github.heheteam.studentbot.metaData.*
import com.github.heheteam.commonlib.SolutionContent
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnSendSolutionState(coursesDistributor: CoursesDistributor) {
    strictlyOn<SendSolutionState> { state ->
        val studentId = state.context.id.toString()
        val courses = coursesDistributor.getAvailableCourses(studentId)
            .filter { !it.second }
        
        if (courses.isEmpty()) {
            val message = bot.send(
                state.context,
                "Сначала запишитесь на курсы!",
                replyMarkup = back()
            )
            waitDataCallbackQuery().first()
            deleteMessage(state.context.id, message.messageId)
            return@strictlyOn MenuState(state.context)
        }

        val courseMessage = bot.send(
            state.context,
            "Выберите курс для отправки решения:",
            replyMarkup = buildCoursesSelector(courses.toMutableList())
        )

        val callback = waitDataCallbackQuery().first()
        if (callback.data == ButtonKey.BACK) {
            deleteMessage(state.context.id, courseMessage.messageId)
            return@strictlyOn MenuState(state.context)
        }

        val courseId = callback.data.split(" ").last()
        state.selectedCourse = courses.first { it.first.id == courseId }.first
        deleteMessage(state.context.id, courseMessage.messageId)

        val promptMessage = bot.send(
            state.context,
            "Напиши решение текстом и я отошлю его на проверку"
        )

        val solution = waitTextMessage().first()
        deleteMessage(state.context.id, promptMessage.messageId)

        val solutionContent = SolutionContent(
            text = solution.content.text,
            fileIds = null
        )
        println("Solution: $solutionContent")
        // TODO: Distribute solution to teacher
        
        val confirmMessage = bot.send(
            state.context,
            "Решение отправлено на проверку!",
            replyMarkup = back()
        )

        waitDataCallbackQuery().first()
        deleteMessage(state.context.id, confirmMessage.messageId)

        MenuState(state.context)
    }
} 