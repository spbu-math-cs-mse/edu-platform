package com.github.heheteam.commonlib.logic.ui

import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.SolutionId
import com.github.michaelbull.result.Result

interface SolutionCourseResolver {
  fun resolveCourse(solutionId: SolutionId): Result<CourseId, Any>
}
