package com.github.heheteam.commonlib.api

import com.github.heheteam.commonlib.EduPlatformError
import com.github.heheteam.commonlib.Grade
import com.github.heheteam.commonlib.Parent
import com.github.heheteam.commonlib.ResolveError
import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.interfaces.GradeTable
import com.github.heheteam.commonlib.interfaces.ParentId
import com.github.heheteam.commonlib.interfaces.ParentStorage
import com.github.heheteam.commonlib.interfaces.ProblemId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.StudentStorage
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import dev.inmo.tgbotapi.types.UserId
import dev.inmo.tgbotapi.utils.mapNotNullValues

class ParentApi
internal constructor(
  private val studentStorage: StudentStorage,
  private val gradeTable: GradeTable,
  private val parentStorage: ParentStorage,
) {
  fun getChildren(parentId: ParentId): List<Student> = studentStorage.getChildren(parentId)

  fun getStudentPerformance(studentId: StudentId): Result<Map<ProblemId, Grade>, EduPlatformError> =
    gradeTable.getStudentPerformance(studentId).map { it.mapNotNullValues() }

  fun tryLoginByTelegramId(id: UserId): Result<Parent, ResolveError<UserId>> =
    parentStorage.resolveByTgId(id)

  fun createParent(): Result<ParentId, EduPlatformError> = parentStorage.createParent()

  fun tryLoginByParentId(parentId: ParentId): Result<Parent, ResolveError<ParentId>> =
    parentStorage.resolveParent(parentId)
}
