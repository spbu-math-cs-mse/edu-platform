package com.github.heheteam.commonlib.api

import com.github.michaelbull.result.Result

interface ResponsibleTeacherResolver {
  fun resolveResponsibleTeacher(problemId: ProblemId): Result<TeacherId, String>
}
