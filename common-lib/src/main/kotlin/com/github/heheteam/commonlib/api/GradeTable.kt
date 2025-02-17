package com.github.heheteam.commonlib.api

import com.github.heheteam.commonlib.Grade
import com.github.heheteam.commonlib.SolutionAssessment
import java.time.LocalDateTime

// bound to a course
interface GradeTable {
  // maps student problem ids to grades
  fun getStudentPerformance(studentId: StudentId): Map<ProblemId, Grade?>


  /**
   * Retrieves the grades of a student for the specified assignments.
   *
   * @param studentId The unique identifier of the student.
   * @param assignmentIds A list of assignment identifiers to fetch grades for.
   * @return A map where each problem ID is associated with its corresponding grade.
   *         A grade is `null` if the solution has not been checked.
   */
  fun getStudentPerformance(
    studentId: StudentId,
    assignmentIds: List<AssignmentId>
  ): Map<ProblemId, Grade?>

  /**
   * Retrieves the grades for all students in the specified course.
   *
   * @param courseId The unique identifier of the course.
   * @return A map where each student ID is associated with a map of problem IDs to their corresponding grades.
   *         A grade is `null` if the solution has not been checked.
   */
  fun getCourseRating(courseId: CourseId): Map<StudentId, Map<ProblemId, Grade?>>

  fun assessSolution(
    solutionId: SolutionId,
    teacherId: TeacherId,
    assessment: SolutionAssessment,
    teacherStatistics: TeacherStatistics,
    timestamp: LocalDateTime = LocalDateTime.now(),
  )

  fun isChecked(solutionId: SolutionId): Boolean
}
