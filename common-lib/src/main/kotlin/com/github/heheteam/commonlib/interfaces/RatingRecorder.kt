package com.github.heheteam.commonlib.interfaces

import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.michaelbull.result.Result

interface RatingRecorder {
  fun createRatingSpreadsheet(
    courseId: CourseId,
    courseName: String,
  ): Result<SpreadsheetId, EduPlatformError>

  fun updateRating(courseId: CourseId)

  fun updateRating(assignmentId: AssignmentId)

  fun updateRating(problemId: ProblemId)

  fun updateRating(submissionId: SubmissionId)
}
