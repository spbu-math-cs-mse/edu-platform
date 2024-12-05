package com.github.heheteam.parentbot.states

import Dialogues
import Keyboards
import ParentCore
import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.api.ParentIdRegistry
import com.github.heheteam.commonlib.api.StudentId
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnMenuState(
  userIdRegistry: ParentIdRegistry,
  core: ParentCore,
) {
  strictlyOn<MenuState> { state ->
    val userId = userIdRegistry.getUserId(state.context.id).value

    val stickerMessage =
      bot.sendSticker(
        state.context,
        Dialogues.typingSticker,
      )

    val menuMessage =
      bot.send(
        state.context,
        Dialogues.menu(),
        replyMarkup = Keyboards.menu(core.getChildren(userId)),
      )

    when (val command = waitDataCallbackQuery().first().data) {
      Keyboards.petDog -> {
        bot.delete(stickerMessage)
        bot.delete(menuMessage)
        bot.sendSticker(state.context, Dialogues.heartSticker)
        bot.send(state.context, Dialogues.petDog())
        return@strictlyOn MenuState(state.context)
      }

      Keyboards.giveFeedback -> {
        bot.delete(menuMessage)
        return@strictlyOn GivingFeedbackState(state.context)
      }

      else -> {
        bot.delete(stickerMessage)
        bot.delete(menuMessage)
        return@strictlyOn ChildPerformanceState(state.context, Student(StudentId(command.toLong())))
      }
    }
  }
}
