package com.github.heheteam.commonlib.api

import com.github.heheteam.commonlib.SolutionInputRequest
import com.github.michaelbull.result.Result

interface ResponsibleTeacherResolver {
  fun resolveResponsibleTeacher(solutionInputRequest: SolutionInputRequest): Result<TeacherId, String>
}
