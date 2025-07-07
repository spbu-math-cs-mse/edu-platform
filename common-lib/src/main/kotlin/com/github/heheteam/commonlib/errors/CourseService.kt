package com.github.heheteam.commonlib.errors

import com.github.heheteam.commonlib.domain.AddStudentStatus
import com.github.heheteam.commonlib.domain.RemoveStudentStatus
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.StudentStorage
import com.github.heheteam.commonlib.repository.CourseRepository
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

class CourseService(
  private val courseRepository: CourseRepository,
  private val studentStorage: StudentStorage,
  private val database: Database,
) {
  fun addStudents(
    courseId: CourseId,
    studentIds: List<StudentId>,
  ): Result<List<AddStudentStatus>, EduPlatformError> =
    transaction(database) {
      binding {
        val course = courseRepository.findById(courseId).bind()
        val students = studentIds.map { studentStorage.resolveStudent(it).bind() }
        val result =
          students.map { if (it != null) course.addStudent(it) else AddStudentStatus.NotAStudent }
        courseRepository.save(course).bind()
        result
      }
    }

  fun removeStudents(
    courseId: CourseId,
    studentIds: List<StudentId>,
  ): Result<List<RemoveStudentStatus>, EduPlatformError> =
    transaction(database) {
      binding {
        val course = courseRepository.findById(courseId).bind()
        val students = studentIds.map { studentStorage.resolveStudent(it).bind() }
        val result =
          students.map {
            if (it != null) course.removeStudent(it) else RemoveStudentStatus.NotAStudent
          }
        courseRepository.save(course).bind()
        result
      }
    }
}
