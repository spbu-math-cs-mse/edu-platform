package com.github.heheteam.commonlib.logic.ui

import com.github.heheteam.commonlib.Solution
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.TeacherId

open class SolutionSendingError

data class NoResponsibleTeacherFor(val solution: Solution) : SolutionSendingError()

data class FailedToResolveSolution(val solution: Solution) : SolutionSendingError()

data class SendToGroupSolutionError(val courseId: CourseId) : SolutionSendingError()

data class SendToTeacherSolutionError(val teacherId: TeacherId) : SolutionSendingError()
