package com.github.heheteam.teacherbot.run

import com.github.heheteam.commonlib.Solution
import com.github.heheteam.commonlib.api.BotEventBus
import com.github.heheteam.commonlib.util.BotState
import com.github.heheteam.commonlib.util.DeveloperOptions
import com.github.heheteam.teacherbot.logic.NewSolutionTeacherNotifier
import com.github.heheteam.teacherbot.logic.TelegramBotControllersRepository
import com.github.heheteam.teacherbot.states.ChooseGroupCourseState
import com.github.heheteam.teacherbot.states.DeveloperStartState
import com.github.heheteam.teacherbot.states.PresetTeacherState
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.bot.setMyCommands
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndFSMAndStartLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.command
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import dev.inmo.tgbotapi.extensions.utils.groupContentMessageOrNull
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.content.TextMessage
import dev.inmo.tgbotapi.utils.RiskFeature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TeacherRunner : KoinComponent {
  private val botEventBus: BotEventBus by inject()
  private val solutionTeacherNotifier: NewSolutionTeacherNotifier by inject()
  private val telegramBotControllersRepository: TelegramBotControllersRepository by inject()

  suspend fun run(botToken: String, developerOptions: DeveloperOptions = DeveloperOptions()) {
    telegramBotWithBehaviourAndFSMAndStartLongPolling(
        botToken,
        CoroutineScope(Dispatchers.IO),
        onStateHandlingErrorHandler = { state, e ->
          println("Thrown error on $state")
          e.printStackTrace()
          state
        },
      ) {
        telegramBotControllersRepository.get().forEach { it.setTelegramBot(this) }
        botEventBus.subscribeToNewSolutionEvent { solution: Solution ->
          solutionTeacherNotifier.notifyNewSolution(solution)
        }
        println(getMe())
        setMyCommands(
          listOf(
            dev.inmo.tgbotapi.types.BotCommand("start", "start"),
            dev.inmo.tgbotapi.types.BotCommand("end", "end"),
          )
        )
        command("start") { startFsm(it, developerOptions) }
        StateRegister().registerTeacherStates(this)

        allUpdatesFlow.subscribeSafelyWithoutExceptions(this) { println(it) }
      }
      .second
      .join()
  }

  @OptIn(RiskFeature::class)
  private suspend fun DefaultBehaviourContextWithFSM<State>.startFsm(
    it: TextMessage,
    developerOptions: DeveloperOptions,
  ) {
    val user = it.from
    val groupContent = it.groupContentMessageOrNull()
    if (groupContent != null) {
      sendMessage(groupContent.chat, "greetings!")
      startChain(ChooseGroupCourseState(groupContent.chat))
    } else if (user != null) {
      val startingState = findStartState(user, developerOptions)
      startChain(startingState)
    }
  }

  private fun findStartState(
    user: User,
    developerOptions: DeveloperOptions,
  ): BotState<out Any?, out Any, out Any> {
    val presetTeacher = developerOptions.presetTeacherId
    return if (presetTeacher != null) {
      PresetTeacherState(user, presetTeacher)
    } else {
      DeveloperStartState(user)
    }
  }
}
