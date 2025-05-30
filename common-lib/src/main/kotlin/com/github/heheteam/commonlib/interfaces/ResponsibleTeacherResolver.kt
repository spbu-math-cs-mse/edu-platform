package com.github.heheteam.commonlib.interfaces

import com.github.heheteam.commonlib.SolutionInputRequest
import com.github.heheteam.commonlib.TeacherResolveError
import com.github.michaelbull.result.Result

internal interface ResponsibleTeacherResolver {
  fun resolveResponsibleTeacher(
    solutionInputRequest: SolutionInputRequest
  ): Result<TeacherId, TeacherResolveError>
}
