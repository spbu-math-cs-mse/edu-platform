package com.github.heheteam.commonlib.api

import com.github.heheteam.commonlib.Grade
import com.github.heheteam.commonlib.Parent
import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.errors.ErrorManagementService
import com.github.heheteam.commonlib.errors.NumberedError
import com.github.heheteam.commonlib.interfaces.GradeTable
import com.github.heheteam.commonlib.interfaces.ParentId
import com.github.heheteam.commonlib.interfaces.ParentStorage
import com.github.heheteam.commonlib.interfaces.ProblemId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.StudentStorage
import com.github.michaelbull.result.Result
import dev.inmo.tgbotapi.types.UserId
import dev.inmo.tgbotapi.utils.mapNotNullValues

class ParentApi
internal constructor(
  private val studentStorage: StudentStorage,
  private val gradeTable: GradeTable,
  private val parentStorage: ParentStorage,
  private val errorManagementService: ErrorManagementService,
) {
  fun getChildren(parentId: ParentId): Result<List<Student>, NumberedError> =
    errorManagementService.serviceBinding { studentStorage.getChildren(parentId).bind() }

  fun getStudentPerformance(studentId: StudentId): Result<Map<ProblemId, Grade>, NumberedError> =
    errorManagementService.serviceBinding {
      gradeTable.getStudentPerformance(studentId).bind().mapNotNullValues()
    }

  fun tryLoginByTelegramId(id: UserId): Result<Parent, NumberedError> =
    errorManagementService.serviceBinding { parentStorage.resolveByTgId(id).bind() }

  fun createParent(): Result<ParentId, NumberedError> =
    errorManagementService.serviceBinding { parentStorage.createParent().bind() }

  fun tryLoginByParentId(parentId: ParentId): Result<Parent, NumberedError> =
    errorManagementService.serviceBinding { parentStorage.resolveParent(parentId).bind() }
}
