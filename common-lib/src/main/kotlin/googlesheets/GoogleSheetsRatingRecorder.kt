package com.github.heheteam.commonlib.googlesheets

import com.github.heheteam.commonlib.api.*
import kotlinx.coroutines.*

class GoogleSheetsRatingRecorder(
  private val googleSheetsService: GoogleSheetsService,
  private val coursesDistributor: CoursesDistributor,
  private val assignmentStorage: AssignmentStorage,
  private val problemStorage: ProblemStorage,
  private val gradeTable: GradeTable,
  private val solutionDistributor: SolutionDistributor,
) : RatingRecorder {
  private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  init {
    coursesDistributor.getCourses().forEach { course ->
      updateRating(course.id)
    }
  }

  override fun updateRating(courseId: CourseId) {
    runBlocking {
      scope.launch {
        googleSheetsService.updateRating(
          coursesDistributor.resolveCourse(courseId).value,
          assignmentStorage.getAssignmentsForCourse(courseId),
          problemStorage.getProblemsFromCourse(courseId),
          coursesDistributor.getStudents(courseId),
          gradeTable.getCourseRating(courseId, solutionDistributor),
        )
      }
    }
  }

  override fun updateRating(problemId: ProblemId) {
    scope.launch {
      val assignmentId = problemStorage.resolveProblem(problemId).value.assignmentId
      val courseId = assignmentStorage.resolveAssignment(assignmentId).value.courseId
      googleSheetsService.updateRating(
        coursesDistributor.resolveCourse(courseId).value,
        assignmentStorage.getAssignmentsForCourse(courseId),
        problemStorage.getProblemsFromCourse(courseId),
        coursesDistributor.getStudents(courseId),
        gradeTable.getCourseRating(courseId, solutionDistributor),
      )
    }
  }
}
