package com.github.heheteam.commonlib.googlesheets

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.EduPlatformError
import com.github.heheteam.commonlib.Grade
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.interfaces.ProblemId
import com.github.heheteam.commonlib.interfaces.SpreadsheetId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.util.ok
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result

class GoogleSheetsServiceDummy : GoogleSheetsService {
  var next = 0L

  override fun createCourseSpreadsheet(course: Course): Result<SpreadsheetId, EduPlatformError> {
    return SpreadsheetId(next++.toString()).ok()
  }

  override fun updateRating(
    courseSpreadsheetId: String,
    course: Course,
    assignments: List<Assignment>,
    problems: List<Problem>,
    students: List<Student>,
    performance: Map<StudentId, Map<ProblemId, Grade?>>,
  ): Result<Unit, EduPlatformError> = Ok(Unit)
}
