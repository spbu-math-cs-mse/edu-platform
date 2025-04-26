package com.github.heheteam.commonlib.interfaces

import kotlinx.datetime.LocalDateTime

interface PersonalDeadlineStorage {
  fun resolveDeadline(studentId: StudentId): LocalDateTime?

  fun updateDeadlineForStudent(studentId: StudentId, newDeadline: LocalDateTime)
}
