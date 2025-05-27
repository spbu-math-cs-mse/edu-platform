package com.github.heheteam.commonlib.api

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.ResolveError
import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.Teacher
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.CourseStorage
import com.github.heheteam.commonlib.interfaces.SolutionId
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.heheteam.commonlib.interfaces.TeacherStorage
import com.github.heheteam.commonlib.logic.AcademicWorkflowService
import com.github.heheteam.commonlib.logic.ui.MenuMessageUpdater
import com.github.michaelbull.result.Result
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.UserId
import kotlinx.datetime.LocalDateTime

class TeacherApi
internal constructor(
  private val courseStorage: CourseStorage,
  private val academicWorkflowService: AcademicWorkflowService,
  private val teacherStorage: TeacherStorage,
  private val menuMessageUpdater: MenuMessageUpdater,
) {
  fun setCourseGroup(courseId: CourseId, chatId: RawChatId): Result<Unit, ResolveError<CourseId>> =
    courseStorage.setCourseGroup(courseId, chatId)

  fun assessSolution(
    solutionId: SolutionId,
    teacherId: TeacherId,
    solutionAssessment: SolutionAssessment,
    timestamp: LocalDateTime,
  ) = academicWorkflowService.assessSolution(solutionId, teacherId, solutionAssessment, timestamp)

  fun tryLoginByTgId(tgId: UserId): Result<Teacher, ResolveError<UserId>> =
    teacherStorage.resolveByTgId(tgId)

  fun createTeacher(firstName: String, lastName: String, tgId: Long): TeacherId =
    teacherStorage.createTeacher(firstName, lastName, tgId)

  fun loginById(teacherId: TeacherId): Result<Teacher, ResolveError<TeacherId>> =
    teacherStorage.resolveTeacher(teacherId)

  fun getTeacherCourses(teacherId: TeacherId): List<Course> =
    courseStorage.getTeacherCourses(teacherId)

  fun resolveCourse(it: CourseId): Result<Course, ResolveError<CourseId>> =
    courseStorage.resolveCourse(it)

  fun updateTgId(teacherId: TeacherId, id: UserId): Result<Unit, ResolveError<TeacherId>> =
    teacherStorage.updateTgId(teacherId, id)

  fun updateTeacherMenuMessage(teacherId: TeacherId): Result<Unit, String> =
    menuMessageUpdater.updateMenuMessageInPersonalChat(teacherId)

  fun updateGroupMenuMessage(courseId: CourseId): Result<Unit, String> =
    menuMessageUpdater.updateMenuMessageInGroupChat(courseId)
}
