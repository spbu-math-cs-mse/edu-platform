package com.github.heheteam.parentbot

import com.github.heheteam.commonlib.Grade
import com.github.heheteam.commonlib.Parent
import com.github.heheteam.commonlib.ResolveError
import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.api.GradeTable
import com.github.heheteam.commonlib.api.ParentId
import com.github.heheteam.commonlib.api.ParentStorage
import com.github.heheteam.commonlib.api.ProblemId
import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.api.StudentStorage
import com.github.michaelbull.result.Result
import dev.inmo.tgbotapi.types.UserId
import dev.inmo.tgbotapi.utils.mapNotNullValues

class ParentApi(
  private val studentStorage: StudentStorage,
  private val gradeTable: GradeTable,
  private val parentStorage: ParentStorage,
) {
  fun getChildren(parentId: ParentId): List<Student> = studentStorage.getChildren(parentId)

  fun getStudentPerformance(studentId: StudentId): Map<ProblemId, Grade> =
    gradeTable.getStudentPerformance(studentId).mapNotNullValues()

  fun tryLoginByTelegramId(id: UserId): Result<Parent, ResolveError<UserId>> =
    parentStorage.resolveByTgId(id)

  fun createParent(): ParentId = parentStorage.createParent()

  fun tryLoginByParentId(parentId: ParentId): Result<Parent, ResolveError<ParentId>> =
    parentStorage.resolveParent(parentId)
}
