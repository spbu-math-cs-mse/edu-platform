package com.github.heheteam.adminbot.states

import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnNotAdminState() {
    strictlyOn<NotAdminState> { state ->
        val callback = waitDataCallbackQuery().first()
        val data = callback.data
        when {
            data == "update" ->
                StartState(state.context)

            else -> NotAdminState(state.context)
        }
    }
}