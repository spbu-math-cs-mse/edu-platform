package com.github.heheteam.teacherbot.states

import com.github.heheteam.commonlib.Solution
import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.SolutionType
import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.util.waitDataCallbackQueryWithUser
import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import com.github.heheteam.teacherbot.Dialogues.noSolutionsToCheck
import com.github.heheteam.teacherbot.Dialogues.solutionInfo
import com.github.heheteam.teacherbot.Keyboards
import com.github.heheteam.teacherbot.TeacherCore
import dev.inmo.tgbotapi.bot.exceptions.CommonRequestException
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.send.media.sendDocument
import dev.inmo.tgbotapi.extensions.api.send.media.sendMediaGroup
import dev.inmo.tgbotapi.extensions.api.send.media.sendPhoto
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.requests.abstracts.InputFile
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.media.TelegramMediaPhoto
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.RiskFeature
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOf

@OptIn(RiskFeature::class, ExperimentalCoroutinesApi::class)
fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnGettingSolutionState(
    core: TeacherCore,
) {
    strictlyOn<GettingSolutionState> { state ->
        val teacherId = state.teacherId
        val solution = core.querySolution(teacherId)

        if (solution == null) {
            handleNoSolution(state)
        } else {
            handleSolution(state, solution, core, teacherId)
        }

        MenuState(state.context, state.teacherId)
    }
}

private suspend fun BehaviourContext.handleNoSolution(state: GettingSolutionState) {
    bot.send(
        state.context,
        noSolutionsToCheck(),
    )
}

private suspend fun BehaviourContext.handleSolution(
    state: GettingSolutionState,
    solution: Solution,
    core: TeacherCore,
    teacherId: TeacherId
) {
    val (getSolution, getMarkup) = handleSolutionMessage(state, solution)
    handleSolutionResponse(state, solution, getSolution, getMarkup, core, teacherId)
}

private suspend fun BehaviourContext.handleSolutionMessage(
    state: GettingSolutionState,
    solution: Solution
): Pair<ContentMessage<*>, ContentMessage<*>?> {
    var getMarkup: ContentMessage<*>? = null
    val getSolution = when (solution.type) {
        SolutionType.TEXT -> handleTextSolution(state, solution)
        SolutionType.PHOTO -> handlePhotoSolution(state, solution)
        SolutionType.PHOTOS -> {
            val result = handlePhotosSolution(state, solution)
            getMarkup = result.second
            result.first
        }
        SolutionType.DOCUMENT -> handleDocumentSolution(state, solution)
    }
    return getSolution to getMarkup
}

private suspend fun BehaviourContext.handleSolutionResponse(
    state: GettingSolutionState,
    solution: Solution,
    getSolution: ContentMessage<*>,
    getMarkup: ContentMessage<*>?,
    core: TeacherCore,
    teacherId: TeacherId
) {
    when (
        val response = flowOf(
            waitDataCallbackQueryWithUser(state.context.id),
            waitTextMessageWithUser(state.context.id)
        ).flattenMerge().first()
    ) {
        is DataCallbackQuery -> {
            when (response.data) {
                Keyboards.goodSolution -> handleGoodSolution(solution, getSolution, core, teacherId)
                Keyboards.badSolution -> handleBadSolution(solution, getSolution, core, teacherId)
                Keyboards.returnBack -> {
                    delete(getSolution)
                    getMarkup?.let { delete(it) }
                }
            }
        }
    }
}

private suspend fun BehaviourContext.handleTextSolution(
    state: GettingSolutionState,
    solution: Solution
): ContentMessage<*> = bot.send(
    state.context,
    solution.content.text!! + "\n\n\n" + solutionInfo(solution),
    replyMarkup = Keyboards.solutionMenu(),
)

private suspend fun BehaviourContext.handlePhotoSolution(
    state: GettingSolutionState,
    solution: Solution
): ContentMessage<*> = bot.sendPhoto(
    state.context,
    InputFile.fromId(solution.content.fileIds!![0]),
    text = if (solution.content.text == null) {
        solutionInfo(solution)
    } else {
        solution.content.text + "\n\n\n" + solutionInfo(solution)
    },
    replyMarkup = Keyboards.solutionMenu(),
)

private suspend fun BehaviourContext.handlePhotosSolution(
    state: GettingSolutionState,
    solution: Solution
): Pair<ContentMessage<*>, ContentMessage<*>> {
    val getSolution = bot.sendMediaGroup(
        state.context,
        listOf(
            TelegramMediaPhoto(
                InputFile.fromId(solution.content.fileIds!![0]),
                solution.content.text,
            ),
        ) + solution.content.fileIds!!
            .map { TelegramMediaPhoto(InputFile.fromId(it)) }
            .drop(1),
    )
    val getMarkup = bot.send(state.context, solutionInfo(solution), replyMarkup = Keyboards.solutionMenu())
    return getSolution to getMarkup
}

private suspend fun BehaviourContext.handleDocumentSolution(
    state: GettingSolutionState,
    solution: Solution
): ContentMessage<*> = bot.sendDocument(
    state.context,
    InputFile.fromId(solution.content.fileIds!![0]),
    text = if (solution.content.text == null) {
        solutionInfo(solution)
    } else {
        solution.content.text + "\n\n\n" + solutionInfo(solution)
    },
    replyMarkup = Keyboards.solutionMenu(),
)

private suspend fun BehaviourContext.handleGoodSolution(
    solution: Solution,
    getSolution: ContentMessage<*>,
    core: TeacherCore,
    teacherId: TeacherId
) {
    try {
        bot.reply(
            ChatId(solution.chatId),
            solution.messageId,
            "good",
        )
    } catch (e: CommonRequestException) {
    }
    deleteMessage(getSolution)
    core.assessSolution(
        solution,
        teacherId,
        SolutionAssessment(1, ""),
    )
}

private suspend fun BehaviourContext.handleBadSolution(
    solution: Solution,
    getSolution: ContentMessage<*>,
    core: TeacherCore,
    teacherId: TeacherId
) {
    try {
        bot.reply(
            ChatId(solution.chatId),
            solution.messageId,
            "bad",
        )
    } catch (e: CommonRequestException) {
    }
    deleteMessage(getSolution)
    core.assessSolution(
        solution,
        teacherId,
        SolutionAssessment(0, ""),
    )
}
