package com.github.heheteam.teacherbot

import com.github.heheteam.commonlib.api.TeacherApi
import com.github.heheteam.commonlib.state.BotState
import com.github.heheteam.commonlib.state.registerState
import com.github.heheteam.commonlib.util.DeveloperOptions
import com.github.heheteam.teacherbot.states.ChooseGroupCourseState
import com.github.heheteam.teacherbot.states.DeveloperStartState
import com.github.heheteam.teacherbot.states.ListeningForSubmissionsGroupState
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
import dev.inmo.tgbotapi.types.BotCommand
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.content.TextMessage
import dev.inmo.tgbotapi.utils.RiskFeature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class TeacherRunner(
  private val botToken: String,
  private val stateRegister: StateRegister,
  private val developerOptions: DeveloperOptions? = DeveloperOptions(),
) {
  suspend fun execute() {
    telegramBotWithBehaviourAndFSMAndStartLongPolling(
        botToken,
        CoroutineScope(Dispatchers.IO),
        onStateHandlingErrorHandler = { state, e ->
          println("Thrown error in TeacherBot on $state")
          e.printStackTrace()
          state
        },
      ) {
        println(getMe())

        setMyCommands(
          listOf(BotCommand("start", "Start bot"), BotCommand("menu", "Resend menu message"))
        )
        command("start") { startFsm(it) }
        stateRegister.registerTeacherStates(this, botToken)

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

  private fun findStartState(user: User): BotState<out Any?, out Any, out Any> =
    if (developerOptions != null) {
      val presetTeacher = developerOptions.presetTeacherId
      if (presetTeacher != null) {
        PresetTeacherState(user, presetTeacher)
      } else {
        DeveloperStartState(user)
      }
    } else {
      StartState(user)
    }
}

class StateRegister(private val teacherApi: TeacherApi) {
  fun registerTeacherStates(context: DefaultBehaviourContextWithFSM<State>, botToken: String) {
    with(context) {
      strictlyOn<ListeningForSubmissionsGroupState> { state ->
        state.lateinitTeacherBotToken = botToken
        state.handle(this, teacherApi)
      }
      registerState<StartState, TeacherApi>(teacherApi)
      registerState<DeveloperStartState, TeacherApi>(teacherApi)
      strictlyOn<MenuState> { state ->
        state.teacherBotToken = botToken
        state.handle(this, teacherApi)
      }
      registerState<PresetTeacherState, TeacherApi>(teacherApi)
      registerState<ChooseGroupCourseState, TeacherApi>(teacherApi)
    }
  }
}
