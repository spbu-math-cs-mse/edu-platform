package com.github.heheteam.commonlib.interfaces

import com.github.heheteam.commonlib.SubmissionInputRequest
import com.github.heheteam.commonlib.errors.TeacherResolveError
import com.github.michaelbull.result.Result

internal interface ResponsibleTeacherResolver {
  fun resolveResponsibleTeacher(
    submissionInputRequest: SubmissionInputRequest
  ): Result<TeacherId, TeacherResolveError>
}
