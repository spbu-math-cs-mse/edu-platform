package com.github.heheteam.commonlib.api

import com.github.heheteam.commonlib.Grade
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.domain.RichCourse
import com.github.heheteam.commonlib.domain.RichParent
import com.github.heheteam.commonlib.errors.CourseService
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.heheteam.commonlib.errors.ErrorManagementService
import com.github.heheteam.commonlib.errors.NumberedError
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.GradeTable
import com.github.heheteam.commonlib.interfaces.ParentId
import com.github.heheteam.commonlib.interfaces.ProblemStorage
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.service.ParentService
import com.github.michaelbull.result.BindingScope
import com.github.michaelbull.result.Result
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.utils.mapNotNullValues

class ParentApi
internal constructor(
  private val gradeTable: GradeTable,
  private val errorManagementService: ErrorManagementService,
  private val parentService: ParentService,
  private val courseService: CourseService,
  private val problemStorage: ProblemStorage,
) : CommonUserApi<ParentId> {
  private fun <V> withErrorManagement(
    block: BindingScope<EduPlatformError>.() -> V
  ): Result<V, NumberedError> {
    return errorManagementService.serviceBinding { block() }
  }

  fun getChildrenOfParent(parentId: ParentId): Result<List<Student>, NumberedError> =
    withErrorManagement {
      parentService.getChildren(parentId).bind()
    }

  fun getStudentPerformance(
    studentId: StudentId,
    courseId: CourseId,
  ): Result<Map<Problem, Grade>, NumberedError> = withErrorManagement {
    gradeTable.getStudentPerformance(studentId, courseId).bind().mapNotNullValues().mapKeys {
      problemStorage.resolveProblem(it.key).bind()
    }
  }

  fun tryLoginByTelegramId(id: RawChatId): Result<RichParent?, NumberedError> =
    errorManagementService.serviceBinding { parentService.tryLoginByTgId(id).bind() }

  fun createParent(
    firstName: String,
    lastName: String,
    tgId: RawChatId,
    from: String?,
  ): Result<RichParent, NumberedError> =
    errorManagementService.serviceBinding {
      parentService.createParent(firstName, lastName, tgId, from).bind()
    }

  fun getStudentCourses(studentId: StudentId): Result<List<RichCourse>, NumberedError> =
    withErrorManagement {
      courseService.getStudentCourses(studentId).bind()
    }

  fun addChild(parentId: ParentId, studentId: StudentId): Result<Unit, NumberedError> =
    withErrorManagement {
      parentService.addChild(parentId, studentId).bind()
    }

  override fun resolveCurrentQuestState(userId: ParentId): Result<String?, NumberedError> =
    withErrorManagement {
      parentService.resolveCurrentQuestState(userId).bind()
    }

  override fun saveCurrentQuestState(
    userId: ParentId,
    questState: String,
  ): Result<Unit, NumberedError> = withErrorManagement {
    parentService.saveCurrentQuestSave(userId, questState).bind()
  }
}
