package com.github.heheteam.commonlib.logic

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.database.DatabaseCourseTokenStorage
import com.github.heheteam.commonlib.domain.AddStudentStatus
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.heheteam.commonlib.errors.TokenError
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.CourseStorage
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.util.raiseError
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import java.util.UUID

class CourseTokenService
internal constructor(
  private val databaseCourseTokenStorage: DatabaseCourseTokenStorage,
  private val courseStorage: CourseStorage,
) {
  fun createToken(courseId: CourseId): String {
    val token = UUID.randomUUID().toString()
    databaseCourseTokenStorage.storeToken(courseId, token)
    return token
  }

  fun regenerateToken(courseId: CourseId): String {
    val newToken = UUID.randomUUID().toString()
    databaseCourseTokenStorage.setNewToken(courseId, newToken)
    return newToken
  }

  fun getCourseIdByToken(token: String): Result<CourseId, TokenError> =
    databaseCourseTokenStorage.getCourseIdByToken(token)

  fun useToken(token: String): Result<Unit, EduPlatformError> = binding {
    val tokenExists = databaseCourseTokenStorage.doesTokenExist(token).bind()
    if (!tokenExists) {
      raiseError(TokenError.TokenNotFound)
    } else {
      Unit
    }
  }

  fun registerStudentForToken(
    studentId: StudentId,
    token: String,
  ): Result<Course?, EduPlatformError> = binding {
    val courseIdResult = getCourseIdByToken(token).bind()
    useToken(token).bind()
    val status = courseStorage.addStudentToCourse(studentId, courseIdResult).bind()
    if (status == AddStudentStatus.Success) {
      courseStorage.resolveCourse(courseIdResult).bind()
    } else {
      null
    }
  }

  fun getTokenForCourse(courseId: CourseId): String? =
    databaseCourseTokenStorage.getTokenForCourse(courseId)
}
