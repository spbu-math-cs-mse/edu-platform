package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.Solution
import com.github.heheteam.commonlib.SolutionContent
import com.github.heheteam.studentbot.StudentCore
import com.github.heheteam.studentbot.metaData.*
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitPhotoMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.message
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.reply_markup
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.text
import dev.inmo.tgbotapi.extensions.utils.photoContentOrNull
import dev.inmo.tgbotapi.types.fileField
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.message.payments.PaidMedia
import dev.inmo.tgbotapi.utils.RiskFeature
import kotlinx.coroutines.flow.first

@OptIn(RiskFeature::class)
fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnSendSolutionState(core: StudentCore) {
    strictlyOn<SendSolutionState> { state ->
        val studentId = core.getUserId(state.context.id)
        val courses =
            core
                .getAvailableCourses(studentId)
                .filter { !it.second }

        if (courses.isEmpty()) {
            val message =
                bot.send(
                    state.context,
                    "Сначала запишитесь на курсы!",
                    replyMarkup = back(),
                )
            waitDataCallbackQuery().first()
            deleteMessage(state.context.id, message.messageId)
            return@strictlyOn MenuState(state.context)
        }

        var courseMessage =
            bot.send(
                state.context,
                "Выберите курс для отправки решения:",
                replyMarkup = buildCoursesSelector(courses.toMutableList()),
            )

        val callback = waitDataCallbackQuery().first()
        if (callback.data == ButtonKey.BACK) {
            deleteMessage(state.context.id, courseMessage.messageId)
            return@strictlyOn MenuState(state.context)
        }

        val courseId = callback.data.split(" ").last()
        state.selectedCourse = courses.first { it.first.id == courseId }.first
        deleteMessage(state.context.id, courseMessage.messageId)

        val typeSelectorMessage = bot.send(
            state.context,
            "Как бы вы хотели отправить решение?",
            replyMarkup = buildSendSolutionSelector()
        )

        val type = waitDataCallbackQuery().first().data
        var lastMessage: ContentMessage<TextContent>? = null

        while (true) {
            if (type == ButtonKey.BACK) {
                deleteMessage(state.context.id, typeSelectorMessage.messageId)
                courseMessage =
                    bot.send(
                        state.context,
                        courseMessage.text.toString(),
                        replyMarkup = courseMessage.reply_markup
                    )
                continue
            } else {
                when (type) {
                    "PHOTO" -> {
                        val promptMessage =
                            bot.send(
                                state.context,
                                "Отправь мне фото я отошлю его на проверку!"
                            )

                        val photos = waitPhotoMessage().first().content.photoContentOrNull()?.mediaCollection

                        if (photos != null) {
                            val photoIds = photos.map { it.fileId.toString() }
                            val solutionContent = SolutionContent(fileIds = photoIds)
                            core.inputSolution(studentId, solutionContent)
                        } else {
                            bot.send(state.context, "Ошибка: ожидалось фото.")
                        }

                        deleteMessage(state.context.id, promptMessage.messageId)
                    }

                    "TEXT" -> {
                        val promptMessage =
                            bot.send(
                                state.context,
                                "Напиши решение текстом и я отошлю его на проверку!"
                            )

                        val solution = waitTextMessage().first()
                        val solutionContent = SolutionContent(text = solution.content.text)
                        core.inputSolution(studentId, solutionContent)

                        deleteMessage(state.context.id, promptMessage.messageId)
                    }

                    else -> {
                        break
                    }
                }

                lastMessage =
                    bot.send(
                        state.context,
                        "Решение отправлено на проверку!",
                        replyMarkup = back()
                    )

                waitDataCallbackQuery().first()
                break
            }
        }

        if (lastMessage != null)
            deleteMessage(state.context.id, lastMessage.messageId)

        MenuState(state.context)
    }
}
