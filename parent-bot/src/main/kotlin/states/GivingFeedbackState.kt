package com.github.heheteam.parentbot.states

import Dialogues
import Keyboards
import com.github.heheteam.parentbot.mockParents
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalCoroutinesApi::class)
fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnGivingFeedbackState() {
  strictlyOn<GivingFeedbackState> { state ->
    if (state.context.username == null) {
      return@strictlyOn null
    }
    val username = state.context.username!!.username
    if (!mockParents.containsKey(username)) {
      return@strictlyOn StartState(state.context)
    }

    val giveFeedbackMessage =
      bot.send(
        state.context,
        Dialogues.giveFeedback(),
        replyMarkup = Keyboards.returnBack(),
      )

    when (val response = flowOf(waitDataCallbackQuery(), waitTextMessage()).flattenMerge().first()) {
      is DataCallbackQuery -> {
        val command = response.data
        if (command == Keyboards.returnBack) {
          delete(giveFeedbackMessage)
        }
      }

      is CommonMessage<*> -> {
        val feedback = response.content
        println("Feedback by user @$username: \n\"$feedback\"")

        bot.sendSticker(
          state.context,
          Dialogues.okSticker,
        )
        bot.send(
          state.context,
          Dialogues.acceptFeedback(),
        )
      }
    }
    MenuState(state.context)
  }
}
