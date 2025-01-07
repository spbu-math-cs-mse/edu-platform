package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.AdminCore
import com.github.heheteam.adminbot.Keyboards.returnBack
import com.github.heheteam.commonlib.util.waitDataCallbackQueryWithUser
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnGetTeachersState(core: AdminCore) {
  strictlyOn<GetTeachersState> { state ->
    val teachers = core.getTeachersBulletList()
    val teachersMessage = bot.send(state.context, text = teachers, replyMarkup = returnBack())

    waitDataCallbackQueryWithUser(state.context.id).first()
    deleteMessage(teachersMessage)
    MenuState(state.context)
  }
}
