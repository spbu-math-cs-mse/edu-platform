package com.github.heheteam.commonlib.mock

import com.github.heheteam.commonlib.SolutionInputRequest
import com.github.heheteam.commonlib.interfaces.ResponsibleTeacherResolver
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.toResultOr

class MockResponsibleTeacherResolver(val teacherId: TeacherId? = null) :
  ResponsibleTeacherResolver {
  override fun resolveResponsibleTeacher(
    solutionInputRequest: SolutionInputRequest
  ): Result<TeacherId, String> = teacherId.toResultOr { "null" }
}
