package com.github.heheteam.parentbot.state

import com.github.heheteam.commonlib.TextWithMediaAttachments
import com.github.heheteam.commonlib.api.ParentApi
import com.github.heheteam.commonlib.interfaces.ParentId
import com.github.heheteam.commonlib.state.InformationState
import com.github.heheteam.commonlib.util.ensureSuccess
import com.github.heheteam.commonlib.util.ok
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.utils.buildEntities

class Start(override val context: User) : State {
  fun handle(api: ParentApi): State {
    val loginTry =
      api.tryLoginByTelegramId(context.id.chatId).ensureSuccess {
        val text = buildEntities {
          +"Случилась ошибка с опознавательным номером ${it.number}\n"
          +"Попробуйте зайти позже, и в случае повторного неуспеха обратитесь к администратору"
        }
        return InformationState<ParentApi, ParentId?>(
          context,
          null,
          { TextWithMediaAttachments(text).ok() },
          this,
        )
      }
    return if (loginTry != null) {
      Menu(context, loginTry.id)
    } else {
      RegisterParent(context)
    }
  }
}
