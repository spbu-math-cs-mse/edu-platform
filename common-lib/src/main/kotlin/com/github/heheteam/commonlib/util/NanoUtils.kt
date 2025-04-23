package com.github.heheteam.commonlib.util

import dev.inmo.tgbotapi.types.RawChatId

fun Long.toRawChatId(): RawChatId = RawChatId(this)
