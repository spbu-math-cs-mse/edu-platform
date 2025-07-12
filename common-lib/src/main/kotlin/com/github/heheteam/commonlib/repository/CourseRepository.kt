package com.github.heheteam.commonlib.repository

import com.github.heheteam.commonlib.domain.RichCourse
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.michaelbull.result.Result

interface CourseRepository {
  fun save(course: RichCourse): Result<RichCourse, EduPlatformError>

  fun findById(courseId: CourseId): Result<RichCourse, EduPlatformError>

  fun findAll(): Result<List<RichCourse>, EduPlatformError>

  fun findByStudent(studentId: StudentId): Result<List<RichCourse>, EduPlatformError>
}
