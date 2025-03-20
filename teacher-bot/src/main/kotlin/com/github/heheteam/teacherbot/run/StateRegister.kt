package com.github.heheteam.teacherbot.run

import com.github.heheteam.commonlib.api.CoursesDistributor
import com.github.heheteam.commonlib.api.TeacherStorage
import com.github.heheteam.commonlib.util.registerState
import com.github.heheteam.teacherbot.logic.SolutionGrader
import com.github.heheteam.teacherbot.logic.TelegramSolutionSenderImpl
import com.github.heheteam.teacherbot.states.ChooseGroupCourseState
import com.github.heheteam.teacherbot.states.DeveloperStartState
import com.github.heheteam.teacherbot.states.ListeningForSolutionsGroupState
import com.github.heheteam.teacherbot.states.MenuState
import com.github.heheteam.teacherbot.states.PresetTeacherState
import com.github.heheteam.teacherbot.states.StartState
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class StateRegister : KoinComponent {
  private val teacherStorage: TeacherStorage by inject()
  private val coursesDistributor: CoursesDistributor by inject()
  private val telegramSolutionSenderImpl: TelegramSolutionSenderImpl by inject()
  private val solutionGrader: SolutionGrader by inject()

  fun registerTeacherStates(context: DefaultBehaviourContextWithFSM<State>) {
    with(context) {
      strictlyOn<ListeningForSolutionsGroupState>({ state ->
        state.execute(this, solutionGrader, telegramSolutionSenderImpl)
      })
      registerState<StartState, TeacherStorage>(teacherStorage)
      registerState<DeveloperStartState, TeacherStorage>(teacherStorage)
      strictlyOn<MenuState> { state -> state.handle(this, teacherStorage, solutionGrader) }
      registerState<PresetTeacherState, CoursesDistributor>(coursesDistributor)
      registerState<ChooseGroupCourseState, CoursesDistributor>(coursesDistributor)
    }
  }
}
