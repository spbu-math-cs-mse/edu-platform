package com.github.heheteam.teacherbot.logic

import com.github.heheteam.commonlib.Solution
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.SolutionId
import com.github.heheteam.commonlib.api.TeacherId

open class SolutionSendingError

data class NoResponsibleTeacherFor(val solution: Solution) : SolutionSendingError()

data class NoTeacherResolvedFor(val id: TeacherId) : SolutionSendingError()

data object UninitializedTeacherBot : SolutionSendingError()

data class NoCourseFoundFor(val solutionId: SolutionId) : SolutionSendingError()

data class SendToGroupSolutionError(val courseId: CourseId) : SolutionSendingError()

data class SendToTeacherSolutionError(val teacherId: TeacherId) : SolutionSendingError()
