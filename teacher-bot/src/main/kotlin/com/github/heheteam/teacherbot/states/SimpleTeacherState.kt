package com.github.heheteam.teacherbot.states

import com.github.heheteam.commonlib.api.TeacherApi
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.heheteam.commonlib.state.SimpleState
import dev.inmo.micro_utils.fsm.common.State

abstract class SimpleTeacherState : SimpleState<TeacherApi, TeacherId>() {
  override fun defaultState(): State = MenuState(context, userId)
}
