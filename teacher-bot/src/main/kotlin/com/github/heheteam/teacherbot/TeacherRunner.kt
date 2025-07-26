package com.github.heheteam.teacherbot

import com.github.heheteam.commonlib.api.TeacherApi
import com.github.heheteam.commonlib.state.registerState
import com.github.heheteam.commonlib.state.registerStateForBotState
import com.github.heheteam.teacherbot.states.AskFirstNameState
import com.github.heheteam.teacherbot.states.AskLastNameState
import com.github.heheteam.teacherbot.states.ChooseGroupCourseState
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
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onContentMessage
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import dev.inmo.tgbotapi.extensions.utils.groupChatOrNull
import dev.inmo.tgbotapi.types.BotCommand
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage
import dev.inmo.tgbotapi.utils.RiskFeature
import io.ktor.http.escapeIfNeeded
import java.time.LocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class TeacherRunner(private val botToken: String, private val stateRegister: StateRegister) {
  suspend fun run() {
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
        onContentMessage { startFsm(it) }
        stateRegister.registerTeacherStates(this, botToken)

        allUpdatesFlow.subscribeSafelyWithoutExceptions(this) {
          println(LocalDateTime.now().toString() + " " + it.toString().escapeIfNeeded())
        }
      }
      .second
      .join()
  }

  @OptIn(RiskFeature::class)
  private suspend fun DefaultBehaviourContextWithFSM<State>.startFsm(it: AccessibleMessage) {
    val user = it.from
    if (it.chat.groupChatOrNull() != null) {
      sendMessage(it.chat, "greetings!")
      startChain(ChooseGroupCourseState(it.chat))
    } else if (user != null) {
      val startingState = StartState(user)
      startChain(startingState)
    }
  }
}

class StateRegister(private val teacherApi: TeacherApi) {
  fun registerTeacherStates(context: DefaultBehaviourContextWithFSM<State>, botToken: String) {
    with(context) {
      strictlyOn<ListeningForSubmissionsGroupState> { state ->
        state.lateinitTeacherBotToken = botToken
        state.handle(this, teacherApi)
      }
      registerStateForBotState<StartState, TeacherApi>(teacherApi)
      registerStateForBotState<StartState, TeacherApi>(teacherApi)
      registerStateForBotState<AskFirstNameState, TeacherApi>(teacherApi)
      registerState<AskLastNameState, TeacherApi>(teacherApi)
      strictlyOn<MenuState> { state ->
        state.teacherBotToken = botToken
        state.handle(this, teacherApi)
      }
      registerStateForBotState<PresetTeacherState, TeacherApi>(teacherApi)
      registerStateForBotState<ChooseGroupCourseState, TeacherApi>(teacherApi)
    }
  }
}
