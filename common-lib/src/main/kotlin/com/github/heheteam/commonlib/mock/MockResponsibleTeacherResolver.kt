package com.github.heheteam.commonlib.mock

import com.github.heheteam.commonlib.SubmissionInputRequest
import com.github.heheteam.commonlib.errors.TeacherResolveError
import com.github.heheteam.commonlib.interfaces.ResponsibleTeacherResolver
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.toResultOr

class MockResponsibleTeacherResolver(val teacherId: TeacherId? = null) :
  ResponsibleTeacherResolver {
  override fun resolveResponsibleTeacher(
    submissionInputRequest: SubmissionInputRequest
  ): Result<TeacherId, TeacherResolveError> = teacherId.toResultOr { TeacherResolveError("mocked") }
}
