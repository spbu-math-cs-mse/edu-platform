package com.github.heheteam.parentbot.states

import Dialogues
import Keyboards
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
        println("Feedback by user @${state.context.username}: \n\"$feedback\"") // TODO: implement receiving feedback

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
    MenuState(state.context, state.parentId)
  }
}
