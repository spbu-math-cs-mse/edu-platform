package com.github.heheteam.teacherbot.logic

import com.github.heheteam.commonlib.api.TeacherId

interface MenuMessageUpdater {
  fun updateMenuMessageInPersonalChat(teacherId: TeacherId)
}
