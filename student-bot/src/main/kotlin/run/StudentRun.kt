package com.github.heheteam.studentbot.run

import com.github.heheteam.commonlib.api.StudentStorage
import com.github.heheteam.commonlib.util.DeveloperOptions
import com.github.heheteam.studentbot.StudentCore
import com.github.heheteam.studentbot.state.*
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndFSMAndStartLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.command
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.utils.RiskFeature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

@OptIn(RiskFeature::class)
suspend fun studentRun(
    botToken: String,
    studentStorage: StudentStorage,
    core: StudentCore,
    developerOptions: DeveloperOptions? = DeveloperOptions(),
) {
    telegramBot(botToken) {
        logger =
            KSLog { level: LogLevel, tag: String?, message: Any, throwable: Throwable? ->
                println(defaultMessageFormatter(level, tag, message, throwable))
            }
    }

    telegramBotWithBehaviourAndFSMAndStartLongPolling(
        botToken,
        CoroutineScope(Dispatchers.IO),
        onStateHandlingErrorHandler = { state, e ->
            println("Thrown error on $state")
            e.printStackTrace()
            state
        },
    ) {
        println(getMe())

        command("start") {
            val user = it.from
            if (user != null) {
                val startingState = findStartState(developerOptions, user)
                startChain(startingState)
            }
        }

        strictlyOnStartState(studentStorage)
        strictlyOnDeveloperStartState(studentStorage)
        strictlyOnMenuState()
        strictlyOnViewState(core)
        strictlyOnSignUpState(core)
        strictlyOnSendSolutionState(core)
        strictlyOnCheckGradesState(core)
        strictlyOnPresetStudentState(core)

        allUpdatesFlow.subscribeSafelyWithoutExceptions(this) {
            println(it)
        }
    }.second.join()
}

private fun findStartState(
    developerOptions: DeveloperOptions?,
    user: User,
) = if (developerOptions != null) {
    val presetStudent = developerOptions.presetStudentId
    if (presetStudent != null) {
        PresetStudentState(user, presetStudent)
    } else {
        DevStartState(user)
    }
} else {
    StartState(user)
}
