package com.github.heheteam.commonlib.googlesheets

import com.github.heheteam.commonlib.api.AssignmentStorage
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.CoursesDistributor
import com.github.heheteam.commonlib.api.GradeTable
import com.github.heheteam.commonlib.api.ProblemId
import com.github.heheteam.commonlib.api.ProblemStorage
import com.github.heheteam.commonlib.api.RatingRecorder
import com.github.heheteam.commonlib.api.SolutionDistributor
import com.github.heheteam.commonlib.api.SolutionId
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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

  init {
    coursesDistributor.getCourses().forEach { course -> updateRating(course.id) }
  }

  override fun updateRating(courseId: CourseId) {
    scope.launch {
      val mutex =
        courseMutexes[courseId]
          ?: run {
            courseMutexes[courseId] = Mutex()
            courseMutexes[courseId]!!
          }
      mutex.withLock {
        googleSheetsService.updateRating(
          coursesDistributor.resolveCourse(courseId).value,
          assignmentStorage.getAssignmentsForCourse(courseId),
          problemStorage.getProblemsFromCourse(courseId),
          coursesDistributor.getStudents(courseId),
          gradeTable.getCourseRating(courseId),
        )
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
