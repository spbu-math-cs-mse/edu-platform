package com.github.heheteam.commonlib.domain

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.heheteam.commonlib.errors.NamedError
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.SpreadsheetId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import dev.inmo.tgbotapi.types.RawChatId

enum class AddStudentStatus {
  ExistsInCourse,
  Success,
  NotAStudent,
}

enum class RemoveStudentStatus {
  Removed,
  NotFoundInCourse,
  NotAStudent,
}

data class RichCourse(
  val id: CourseId,
  var description: String,
  var spreadsheetId: SpreadsheetId?,
  var groupChatId: RawChatId?,
  val students: MutableList<StudentId>,
  val teachers: MutableList<TeacherId>,
) {
  fun addStudent(studentId: StudentId): Result<Unit, EduPlatformError> {
    students.add(studentId)
    return Ok(Unit)
  }

  fun addStudent(student: Student): AddStudentStatus {
    if (student.id in students) {
      return AddStudentStatus.ExistsInCourse
    }
    students.add(student.id)
    return AddStudentStatus.Success
  }

  fun removeStudent(student: Student): RemoveStudentStatus {
    return removeStudent(student.id)
  }

  fun removeStudent(studentId: StudentId): RemoveStudentStatus {
    return if (students.remove(studentId)) {
      RemoveStudentStatus.Removed
    } else {
      RemoveStudentStatus.NotFoundInCourse
    }
  }

  fun addTeacher(teacherId: TeacherId): Result<Unit, EduPlatformError> {
    teachers.add(teacherId)
    return Ok(Unit)
  }

  fun removeTeacher(teacherId: TeacherId): Result<Unit, EduPlatformError> {
    if (!teachers.remove(teacherId)) {
      return Err(NamedError("Teacher not found in course"))
    }
    return Ok(Unit)
  }

  fun toLegacy(): Course {
    return Course(id = this.id, name = this.description)
  }
}
