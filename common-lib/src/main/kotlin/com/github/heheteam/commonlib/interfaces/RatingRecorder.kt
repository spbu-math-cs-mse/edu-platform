package com.github.heheteam.commonlib.interfaces

import com.github.heheteam.commonlib.EduPlatformError
import com.github.michaelbull.result.Result

interface RatingRecorder {
  fun createRatingSpreadsheet(courseId: CourseId): Result<SpreadsheetId, EduPlatformError>

  fun updateRating(courseId: CourseId)

  fun updateRating(problemId: ProblemId)

  fun updateRating(submissionId: SubmissionId)
}
