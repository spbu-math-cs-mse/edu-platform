package com.github.heheteam.studentbot

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.ResolveError
import com.github.heheteam.commonlib.SolutionInputRequest
import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.api.AssignmentId
import com.github.heheteam.commonlib.api.AssignmentStorage
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.CoursesDistributor
import com.github.heheteam.commonlib.api.ProblemGrade
import com.github.heheteam.commonlib.api.ProblemStorage
import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.api.StudentStorage
import com.github.heheteam.commonlib.logic.AcademicWorkflowService
import com.github.michaelbull.result.Result
import dev.inmo.tgbotapi.types.UserId

class StudentApi(
  private val coursesDistributor: CoursesDistributor,
  private val problemStorage: ProblemStorage,
  private val assignmentStorage: AssignmentStorage,
  private val academicWorkflowService: AcademicWorkflowService,
  private val studentStorage: StudentStorage,
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

  fun loginByTgId(tgId: UserId): Result<Student, ResolveError<UserId>> =
    studentStorage.resolveByTgId(tgId)

  fun loginById(studentId: StudentId): Result<Student, ResolveError<StudentId>> =
    studentStorage.resolveStudent(studentId)

  fun createStudent(name: String, surname: String, tgId: Long): StudentId {
    return studentStorage.createStudent(name, surname, tgId)
  }

  fun getProblemsWithAssignmentsFromCourse(courseId: CourseId): Map<Assignment, List<Problem>> =
    problemStorage.getProblemsWithAssignmentsFromCourse(courseId)
}
