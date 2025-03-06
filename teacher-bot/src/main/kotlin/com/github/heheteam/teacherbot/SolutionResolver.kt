package com.github.heheteam.teacherbot

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.Solution
import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.api.AssignmentId
import com.github.heheteam.commonlib.api.AssignmentStorage
import com.github.heheteam.commonlib.api.ProblemId
import com.github.heheteam.commonlib.api.ProblemStorage
import com.github.heheteam.commonlib.api.ResolveError
import com.github.heheteam.commonlib.api.SolutionDistributor
import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.api.StudentStorage
import com.github.heheteam.commonlib.api.TeacherId
import com.github.michaelbull.result.Result
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SolutionResolver : KoinComponent {
  val solutionDistributor: SolutionDistributor by inject()
  private val problemStorage: ProblemStorage by inject()
  private val assignmentStorage: AssignmentStorage by inject()
  private val studentStorage: StudentStorage by inject()

  fun querySolution(teacherId: TeacherId): Solution? =
    solutionDistributor.querySolution(teacherId).value

  fun resolveAssignment(
    assignmentId: AssignmentId
  ): Result<Assignment, ResolveError<AssignmentId>> =
    assignmentStorage.resolveAssignment(assignmentId)

  fun resolveProblem(problemId: ProblemId): Result<Problem, ResolveError<ProblemId>> =
    problemStorage.resolveProblem(problemId)

  fun resolveStudent(studentId: StudentId): Result<Student, ResolveError<StudentId>> =
    studentStorage.resolveStudent(studentId)
}
