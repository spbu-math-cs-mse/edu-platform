package com.github.heheteam.commonlib.interfaces

import com.github.heheteam.commonlib.CreateError
import com.github.michaelbull.result.Result

interface RatingRecorder {
  fun createRatingSpreadsheet(courseId: CourseId): Result<SpreadsheetId, CreateError>

  fun updateRating(courseId: CourseId)

  fun updateRating(problemId: ProblemId)

  fun updateRating(solutionId: SolutionId)
}
