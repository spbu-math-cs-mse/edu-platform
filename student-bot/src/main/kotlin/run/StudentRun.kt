package com.github.heheteam.studentbot.run

import com.github.heheteam.commonlib.api.StudentStorage
import com.github.heheteam.commonlib.api.toCourseId
import com.github.heheteam.commonlib.util.DeveloperOptions
import com.github.heheteam.studentbot.StudentCore
import com.github.heheteam.studentbot.state.*
import dev.inmo.kslog.common.*
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.tgbotapi.bot.exceptions.CommonRequestException
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndFSMAndStartLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.command
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.toChatId
import dev.inmo.tgbotapi.utils.RiskFeature
import korlibs.time.fromSeconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import kotlin.time.Duration

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
        startScheduledMessageService(core)
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

private fun DefaultBehaviourContextWithFSM<BotState>.startScheduledMessageService(
  core: StudentCore,
) {
  launch {
    core.tmpSendSampleMessage(
      1L.toCourseId(),
      LocalDateTime.now().plusSeconds(10),
    )
    val updateFrequency = Duration.fromSeconds(1)
    while (true) {
      val messagesToSend =
        core.sendMessagesIfExistUnsent(LocalDateTime.now())
      logger.info(messagesToSend)
      for ((chatId, content) in messagesToSend) {
        try {
          bot.send(
            RawChatId(chatId).toChatId(),
            content,
          )
        } catch (e: CommonRequestException) {
          val message =
            "Failed to send scheduled message to chat id $chatId: ${e.message}"
          if (chatId > 0) {
            logger.error(message)
          } else {
            logger.warning(message)
          }
        }
      }
      delay(updateFrequency)
    }
  }
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
