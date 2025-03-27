package com.github.heheteam.commonlib.googlesheets

import com.github.heheteam.commonlib.CreateError
import com.github.heheteam.commonlib.api.AssignmentStorage
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.CoursesDistributor
import com.github.heheteam.commonlib.api.GradeTable
import com.github.heheteam.commonlib.api.ProblemId
import com.github.heheteam.commonlib.api.ProblemStorage
import com.github.heheteam.commonlib.api.RatingRecorder
import com.github.heheteam.commonlib.api.SolutionDistributor
import com.github.heheteam.commonlib.api.SolutionId
import com.github.heheteam.commonlib.api.SpreadsheetId
import com.github.heheteam.commonlib.util.toUrl
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import java.util.concurrent.ConcurrentHashMap
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val DELAY_IN_MILLISECONDS: Long = 1000

class GoogleSheetsRatingRecorder(
  private val googleSheetsService: GoogleSheetsService,
  private val coursesDistributor: CoursesDistributor,
  private val assignmentStorage: AssignmentStorage,
  private val problemStorage: ProblemStorage,
  private val gradeTable: GradeTable,
  private val solutionDistributor: SolutionDistributor,
) : RatingRecorder {
  private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  private val courseMutexes = ConcurrentHashMap<CourseId, Mutex>()
  private val willBeUpdated = ConcurrentHashMap<CourseId, Boolean>()

  override fun createRatingSpreadsheet(courseId: CourseId): Result<SpreadsheetId, CreateError> {
    val course = coursesDistributor.resolveCourse(courseId).value
    val spreadsheetId =
      try {
        googleSheetsService.createCourseSpreadsheet(course)
      } catch (e: java.io.IOException) {
        return Err(CreateError("Google Spreadheet", e.message))
      }
    coursesDistributor.updateCourseSpreadsheetId(courseId, spreadsheetId)
    println(
      "Created spreadsheet ${spreadsheetId.toUrl()} for course \"${course.name}\" (id: $courseId)"
    )
    updateRating(courseId)
    return Ok(spreadsheetId)
  }

  override fun updateRating(courseId: CourseId) {
    scope.launch {
      val mutex = courseMutexes.computeIfAbsent(courseId) { Mutex() }
      if (!willBeUpdated.computeIfAbsent(courseId) { false }) {
        willBeUpdated.replace(courseId, true)
        mutex.withLock {
          willBeUpdated.replace(courseId, false)
          val elapsedTime = measureTimeMillis {
            coursesDistributor.resolveCourseWithSpreadsheetId(courseId).map {
              (course, spreadsheetId) ->
              googleSheetsService.updateRating(
                spreadsheetId.id,
                course,
                assignmentStorage.getAssignmentsForCourse(courseId),
                problemStorage.getProblemsFromCourse(courseId),
                coursesDistributor.getStudents(courseId),
                gradeTable.getCourseRating(courseId),
              )
            }
          }
          delay(DELAY_IN_MILLISECONDS - elapsedTime)
        }
      }
    }
  }

  override fun updateRating(problemId: ProblemId) {
    scope.launch {
      val assignmentId = problemStorage.resolveProblem(problemId).value.assignmentId
      val courseId = assignmentStorage.resolveAssignment(assignmentId).value.courseId
      updateRating(courseId)
    }
  }

  override fun updateRating(solutionId: SolutionId) {
    scope.launch {
      val problemId = solutionDistributor.resolveSolution(solutionId).value.problemId
      val assignmentId = problemStorage.resolveProblem(problemId).value.assignmentId
      val courseId = assignmentStorage.resolveAssignment(assignmentId).value.courseId
      updateRating(courseId)
    }
  }
}
