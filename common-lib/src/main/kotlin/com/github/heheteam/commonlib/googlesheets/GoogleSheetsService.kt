package com.github.heheteam.commonlib.googlesheets

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.Grade
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.interfaces.ProblemId
import com.github.heheteam.commonlib.interfaces.SpreadsheetId
import com.github.heheteam.commonlib.interfaces.StudentId

data class RawCourseSheetData(
  val assignments: List<Assignment>,
  val problems: List<Problem>,
  val students: List<Student>,
  val performance: Map<StudentId, Map<ProblemId, Grade?>>,
)

interface GoogleSheetsService {
  fun createCourseSpreadsheet(course: Course): SpreadsheetId

  fun updateRating(
    courseSpreadsheetId: String,
    course: Course,
    rawCourseSheetData: RawCourseSheetData,
  )
}
