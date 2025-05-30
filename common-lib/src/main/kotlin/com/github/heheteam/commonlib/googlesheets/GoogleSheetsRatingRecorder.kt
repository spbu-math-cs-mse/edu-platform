package com.github.heheteam.commonlib.googlesheets

import com.github.heheteam.commonlib.CreateError
import com.github.heheteam.commonlib.asEduPlatformError
import com.github.heheteam.commonlib.interfaces.AssignmentStorage
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.CourseStorage
import com.github.heheteam.commonlib.interfaces.ProblemId
import com.github.heheteam.commonlib.interfaces.ProblemStorage
import com.github.heheteam.commonlib.interfaces.RatingRecorder
import com.github.heheteam.commonlib.interfaces.SolutionDistributor
import com.github.heheteam.commonlib.interfaces.SolutionId
import com.github.heheteam.commonlib.interfaces.SpreadsheetId
import com.github.heheteam.commonlib.logic.AcademicWorkflowLogic
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

class GoogleSheetsRatingRecorder
internal constructor(
  private val googleSheetsService: GoogleSheetsService,
  private val courseStorage: CourseStorage,
  private val assignmentStorage: AssignmentStorage,
  private val problemStorage: ProblemStorage,
  private val solutionDistributor: SolutionDistributor,
  private val academicWorkflowLogic: AcademicWorkflowLogic,
) : RatingRecorder {
  private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  private val courseMutexes = ConcurrentHashMap<CourseId, Mutex>()
  private val willBeUpdated = ConcurrentHashMap<CourseId, Boolean>()

  override fun createRatingSpreadsheet(courseId: CourseId): Result<SpreadsheetId, CreateError> {
    val course = courseStorage.resolveCourse(courseId).value
    val spreadsheetId =
      try {
        googleSheetsService.createCourseSpreadsheet(course)
      } catch (e: java.io.IOException) {
        return Err(CreateError("Google Spreadheet", e.message, causedBy = e.asEduPlatformError()))
      }
    courseStorage.updateCourseSpreadsheetId(courseId, spreadsheetId)
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
            courseStorage.resolveCourseWithSpreadsheetId(courseId).map { (course, spreadsheetId) ->
              googleSheetsService.updateRating(
                spreadsheetId.long,
                course,
                assignmentStorage.getAssignmentsForCourse(courseId),
                problemStorage.getProblemsFromCourse(courseId),
                courseStorage.getStudents(courseId),
                academicWorkflowLogic.getCourseRating(courseId),
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
