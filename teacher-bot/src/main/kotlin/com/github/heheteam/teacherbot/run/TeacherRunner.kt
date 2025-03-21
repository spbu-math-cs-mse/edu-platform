package com.github.heheteam.teacherbot.run

import com.github.heheteam.commonlib.Solution
import com.github.heheteam.commonlib.api.BotEventBus
import com.github.heheteam.commonlib.api.CoursesDistributor
import com.github.heheteam.commonlib.api.TeacherStorage
import com.github.heheteam.commonlib.util.BotState
import com.github.heheteam.commonlib.util.DeveloperOptions
import com.github.heheteam.commonlib.util.registerState
import com.github.heheteam.teacherbot.logic.MenuMessageUpdater
import com.github.heheteam.teacherbot.logic.NewSolutionTeacherNotifier
import com.github.heheteam.teacherbot.logic.SolutionGrader
import com.github.heheteam.teacherbot.logic.TelegramBotController
import com.github.heheteam.teacherbot.logic.TelegramSolutionSenderImpl
import com.github.heheteam.teacherbot.states.ChooseGroupCourseState
import com.github.heheteam.teacherbot.states.DeveloperStartState
import com.github.heheteam.teacherbot.states.ListeningForSolutionsGroupState
import com.github.heheteam.teacherbot.states.MenuState
import com.github.heheteam.teacherbot.states.PresetTeacherState
import com.github.heheteam.teacherbot.states.StartState
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

class TeacherRunner(
  private val botToken: String,
  private val botEventBus: BotEventBus,
  private val stateRegister: StateRegister,
  private val developerOptions: DeveloperOptions = DeveloperOptions(),
) {
  suspend fun execute(
    solutionTeacherNotifier: NewSolutionTeacherNotifier,
    telegramBotControllers: List<TelegramBotController>,
  ) {
    telegramBotWithBehaviourAndFSMAndStartLongPolling(
        botToken,
        CoroutineScope(Dispatchers.IO),
        onStateHandlingErrorHandler = { state, e ->
          println("Thrown error on $state")
          e.printStackTrace()
          state
        },
      ) {
        telegramBotControllers.forEach { it.setTelegramBot(this) }
        botEventBus.subscribeToNewSolutionEvent { solution: Solution ->
          solutionTeacherNotifier.notifyNewSolution(solution)
        }
        println(getMe())
        setMyCommands(
          listOf(
            dev.inmo.tgbotapi.types.BotCommand("start", "start"),
            dev.inmo.tgbotapi.types.BotCommand("end", "endCommand"),
          )
        )
        command("start") { startFsm(it) }
        stateRegister.registerTeacherStates(this)

        allUpdatesFlow.subscribeSafelyWithoutExceptions(this) { println(it) }
      }
      .second
      .join()
  }

  @OptIn(RiskFeature::class)
  private suspend fun DefaultBehaviourContextWithFSM<State>.startFsm(it: TextMessage) {
    val user = it.from
    val groupContent = it.groupContentMessageOrNull()
    if (groupContent != null) {
      sendMessage(groupContent.chat, "greetings!")
      startChain(ChooseGroupCourseState(groupContent.chat))
    } else if (user != null) {
      val startingState = findStartState(user)
      startChain(startingState)
    }
  }

  private fun findStartState(user: User): BotState<out Any?, out Any, out Any> {
    val presetTeacher = developerOptions.presetTeacherId
    return if (presetTeacher != null) {
      PresetTeacherState(user, presetTeacher)
    } else {
      DeveloperStartState(user)
    }
  }
}

class StateRegister(
  private val teacherStorage: TeacherStorage,
  private val coursesDistributor: CoursesDistributor,
  private val telegramSolutionSenderImpl: TelegramSolutionSenderImpl,
  private val solutionGrader: SolutionGrader,
  private val menuMessageUpdater: MenuMessageUpdater,
) {
  fun registerTeacherStates(context: DefaultBehaviourContextWithFSM<State>) {
    with(context) {
      strictlyOn<ListeningForSolutionsGroupState>({ state ->
        state.execute(this, solutionGrader, telegramSolutionSenderImpl)
      })
      registerState<StartState, TeacherStorage>(teacherStorage)
      registerState<DeveloperStartState, TeacherStorage>(teacherStorage)
      strictlyOn<MenuState> { state ->
        state.handle(this, teacherStorage, solutionGrader, menuMessageUpdater)
      }
      registerState<PresetTeacherState, CoursesDistributor>(coursesDistributor)
      registerState<ChooseGroupCourseState, CoursesDistributor>(coursesDistributor)
    }
  }
}
