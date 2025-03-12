package com.github.heheteam.teacherbot.logic

import com.github.heheteam.commonlib.api.SolutionId

interface MenuMessageUpdater {
  fun updateMenuMessageInPersonalChat(solutionId: SolutionId)
}
