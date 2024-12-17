package com.github.heheteam.teacherbot.run

import com.github.heheteam.commonlib.api.TeacherStorage
import com.github.heheteam.commonlib.util.DeveloperOptions
import com.github.heheteam.teacherbot.TeacherCore
import com.github.heheteam.teacherbot.state.strictlyOnCheckGradesState
import com.github.heheteam.teacherbot.state.strictlyOnPresetTeacherState
import com.github.heheteam.teacherbot.states.*
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
suspend fun teacherRun(
  botToken: String,
  teacherStorage: TeacherStorage,
  core: TeacherCore,
  developerOptions: DeveloperOptions? = DeveloperOptions(),
) {
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
      val user = it.from
      if (user != null) {
        val startingState = findStartState(developerOptions, user)
        startChain(startingState)
      }
    }

    strictlyOnStartState(
      teacherStorage,
      isDeveloperRun = true,
    )
    strictlyOnMenuState(core)
    strictlyOnGettingSolutionState(core)
    strictlyOnCheckGradesState(core)
    strictlyOnPresetTeacherState(core)

    allUpdatesFlow.subscribeSafelyWithoutExceptions(this) {
      println(it)
    }
  }.second.join()
}

private fun findStartState(
  developerOptions: DeveloperOptions?,
  user: User,
) = if (developerOptions != null) {
  val presetTeacher = developerOptions.presetTeacherId
  if (presetTeacher != null) {
    PresetTeacherState(user, presetTeacher)
  } else {
    StartState(user)
  }
} else {
  StartState(user)
}
