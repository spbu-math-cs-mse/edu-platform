package com.github.heheteam.commonlib.interfaces

import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.michaelbull.result.Result
import kotlinx.datetime.LocalDateTime

interface PersonalDeadlineStorage {
  fun resolveDeadline(studentId: StudentId): LocalDateTime?

  fun updateDeadlineForStudent(
    studentId: StudentId,
    newDeadline: LocalDateTime,
  ): Result<Unit, EduPlatformError>
}
