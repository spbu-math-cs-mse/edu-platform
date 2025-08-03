package com.github.heheteam.commonlib.api

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.SubmissionAssessment
import com.github.heheteam.commonlib.Teacher
import com.github.heheteam.commonlib.errors.ErrorManagementService
import com.github.heheteam.commonlib.errors.NumberedError
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.CourseStorage
import com.github.heheteam.commonlib.interfaces.QuizId
import com.github.heheteam.commonlib.interfaces.SubmissionId
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.heheteam.commonlib.interfaces.TeacherStorage
import com.github.heheteam.commonlib.logic.AcademicWorkflowService
import com.github.heheteam.commonlib.logic.ui.MenuMessageUpdater
import com.github.heheteam.commonlib.quiz.QuizActivationResult
import com.github.heheteam.commonlib.quiz.QuizMetaInformation
import com.github.heheteam.commonlib.quiz.QuizService
import com.github.heheteam.commonlib.quiz.RichQuiz
import com.github.michaelbull.result.Result
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.UserId
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime

@Suppress("LongParameterList", "TooManyFunctions") // the fate of api class
class TeacherApi
internal constructor(
  private val courseStorage: CourseStorage,
  private val academicWorkflowService: AcademicWorkflowService,
  private val teacherStorage: TeacherStorage,
  private val menuMessageUpdater: MenuMessageUpdater,
  private val errorManagementService: ErrorManagementService,
  private val quizService: QuizService,
) {
  fun setCourseGroup(courseId: CourseId, chatId: RawChatId): Result<Unit, NumberedError> =
    errorManagementService.serviceBinding { courseStorage.setCourseGroup(courseId, chatId).bind() }

  fun createQuiz(quizMetaInformation: QuizMetaInformation): Result<QuizId, NumberedError> =
    errorManagementService.serviceBinding { quizService.create(quizMetaInformation).bind() }

  fun retrieveQuizzes(courseId: CourseId): Result<List<RichQuiz>, NumberedError> =
    errorManagementService.serviceBinding { quizService.retrieve(courseId).bind() }

  suspend fun activateQuiz(
    quizId: QuizId,
    currentTime: Instant,
  ): Result<QuizActivationResult, NumberedError> =
    errorManagementService.coroutineServiceBinding {
      quizService.activateQuiz(quizId, currentTime).bind()
    }

  suspend fun updateQuizzesStati(currentTime: Instant): Result<Unit, NumberedError> =
    errorManagementService.coroutineServiceBinding {
      quizService.updateQuizzesStati(currentTime).bind()
    }

  suspend fun assessSubmission(
    submissionId: SubmissionId,
    teacherId: TeacherId,
    submissionAssessment: SubmissionAssessment,
    timestamp: LocalDateTime,
  ) =
    academicWorkflowService.assessSubmission(
      submissionId,
      teacherId,
      submissionAssessment,
      timestamp,
    )

  fun createTeacher(firstName: String, lastName: String, tgId: Long): TeacherId =
    teacherStorage.createTeacher(firstName, lastName, tgId)

  fun loginByTgId(tgId: UserId): Result<Teacher?, NumberedError> =
    errorManagementService.serviceBinding { teacherStorage.resolveByTgId(tgId).bind() }

  fun loginById(teacherId: TeacherId): Result<Teacher, NumberedError> =
    errorManagementService.serviceBinding { teacherStorage.resolveTeacher(teacherId).bind() }

  fun getTeacherCourses(teacherId: TeacherId): Result<List<Course>, NumberedError> =
    errorManagementService.serviceBinding { courseStorage.getTeacherCourses(teacherId).bind() }

  fun getTeacherCoursesForQuiz(teacherId: TeacherId): Result<List<Course>, NumberedError> =
    getTeacherCourses(teacherId)

  fun resolveCourse(it: CourseId): Result<Course, NumberedError> =
    errorManagementService.serviceBinding { courseStorage.resolveCourse(it).bind() }

  fun updateTgId(teacherId: TeacherId, id: UserId): Result<Unit, NumberedError> =
    errorManagementService.serviceBinding { teacherStorage.updateTgId(teacherId, id).bind() }

  suspend fun updateTeacherMenuMessage(teacherId: TeacherId): Result<Unit, NumberedError> =
    errorManagementService.coroutineServiceBinding {
      menuMessageUpdater.updateMenuMessageInPersonalChat(teacherId).bind()
    }

  suspend fun updateGroupMenuMessage(courseId: CourseId): Result<Unit, NumberedError> =
    errorManagementService.coroutineServiceBinding {
      menuMessageUpdater.updateMenuMessageInGroupChat(courseId).bind()
    }
}
