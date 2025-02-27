package com.github.heheteam.commonlib.mock

import com.github.heheteam.commonlib.api.ProblemId
import com.github.heheteam.commonlib.api.ResponsibleTeacherResolver
import com.github.heheteam.commonlib.api.TeacherId
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.toResultOr

class MockResponsibleTeacherResolver(val teacherId: TeacherId? = null) :
  ResponsibleTeacherResolver {
  override fun resolveResponsibleTeacher(problemId: ProblemId): Result<TeacherId, String> =
    teacherId.toResultOr { "null" }
}
