package com.github.heheteam.studentbot

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.SolutionInputRequest
import com.github.heheteam.commonlib.api.AssignmentId
import com.github.heheteam.commonlib.api.AssignmentStorage
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.CoursesDistributor
import com.github.heheteam.commonlib.api.ProblemGrade
import com.github.heheteam.commonlib.api.ProblemStorage
import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.logic.AcademicWorkflowService

// this class represents a service given by the bot;
// students ids are parameters in this class
class StudentApi(
  private val coursesDistributor: CoursesDistributor,
  private val problemStorage: ProblemStorage,
  private val assignmentStorage: AssignmentStorage,
  private val academicWorkflowService: AcademicWorkflowService,
) {
  fun getGradingForAssignment(
    assignmentId: AssignmentId,
    studentId: StudentId,
  ): List<Pair<Problem, ProblemGrade>> {
    return academicWorkflowService.getGradingsForAssignment(assignmentId, studentId)
  }

  fun getStudentCourses(studentId: StudentId): List<Course> =
    coursesDistributor.getStudentCourses(studentId)

  fun getCourseAssignments(courseId: CourseId): List<Assignment> =
    assignmentStorage.getAssignmentsForCourse(courseId)

  fun applyForCourse(studentId: StudentId, courseId: CourseId) =
    coursesDistributor.addStudentToCourse(studentId, courseId)

  fun inputSolution(solutionInputRequest: SolutionInputRequest) {
    academicWorkflowService.sendSolution(solutionInputRequest)
  }

  fun getProblemsFromAssignment(assignment: Assignment): List<Problem> =
    problemStorage.getProblemsFromAssignment(assignment.id)
}
