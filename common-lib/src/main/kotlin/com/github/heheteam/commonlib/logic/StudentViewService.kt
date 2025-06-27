package com.github.heheteam.commonlib.logic

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.heheteam.commonlib.interfaces.AssignmentId
import com.github.heheteam.commonlib.interfaces.AssignmentStorage
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.CourseStorage
import com.github.heheteam.commonlib.interfaces.ProblemStorage
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.michaelbull.result.Result

/** Represents a readonly information that might be needeed in student's UI */
class StudentViewService
internal constructor(
  private val courseStorage: CourseStorage,
  private val problemStorage: ProblemStorage,
  private val assignmentStorage: AssignmentStorage,
) {

  fun getCourseAssignments(courseId: CourseId): Result<List<Assignment>, EduPlatformError> =
    assignmentStorage.getAssignmentsForCourse(courseId)

  fun getStudentCourses(studentId: StudentId): Result<List<Course>, EduPlatformError> =
    courseStorage.getStudentCourses(studentId)

  fun getProblemsFromAssignment(
    assignmentId: AssignmentId
  ): Result<List<Problem>, EduPlatformError> =
    problemStorage.getProblemsFromAssignment(assignmentId)

  fun getAllCourses(): Result<List<Course>, EduPlatformError> = courseStorage.getCourses()
}
