package com.github.heheteam.commonlib.logic.ui

import com.github.heheteam.commonlib.api.TeacherId

interface MenuMessageUpdater {
  fun updateMenuMessageInPersonalChat(teacherId: TeacherId)
}
