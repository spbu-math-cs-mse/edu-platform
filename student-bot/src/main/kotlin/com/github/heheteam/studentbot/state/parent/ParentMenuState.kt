package com.github.heheteam.studentbot.state.parent

import com.github.heheteam.commonlib.interfaces.ParentId
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.types.chat.User

class ParentMenuState(override val context: User, val userId: ParentId) : State
