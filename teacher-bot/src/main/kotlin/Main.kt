package com.github.heheteam.teacherbot

import com.github.heheteam.teacherbot.state.*
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndFSMAndStartLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.command
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import dev.inmo.tgbotapi.utils.RiskFeature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * @param args bot token and telegram @username for mocking data.
 */
@OptIn(RiskFeature::class)
suspend fun main(vararg args: String) {
    val botToken = args.first()
    val gradeTable = MockGradeTable()
    mockTgUsername = args[1]
    telegramBot(botToken) {
        logger =
            KSLog { level: LogLevel, tag: String?, message: Any, throwable: Throwable? ->
                println(defaultMessageFormatter(level, tag, message, throwable))
            }
    }

    telegramBotWithBehaviourAndFSMAndStartLongPolling<BotState>(
        botToken,
        CoroutineScope(Dispatchers.IO),
        onStateHandlingErrorHandler = { state, e ->
            println("Thrown error on $state")
            e.printStackTrace()
            state
        },
    ) {
        println(getMe())

        command(
            "start",
        ) {
            if (it.from != null) {
                startChain(StartState(it.from!!))
            }
        }

        strictlyOnStartState()
        strictlyOnMenuState()
//    strictlyOnTestSendingSolutionState()
        strictlyOnGettingSolutionState()
        strictlyOnCheckGradesState(gradeTable)

        allUpdatesFlow.subscribeSafelyWithoutExceptions(this) {
            println(it)
        }
    }.second.join()
}
