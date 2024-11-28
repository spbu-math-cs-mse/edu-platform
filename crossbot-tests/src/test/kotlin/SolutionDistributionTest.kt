package com.github.heheteam.commonlib

import com.github.heheteam.commonlib.api.AssignmentStorage
import com.github.heheteam.commonlib.api.ProblemStorage
import com.github.heheteam.commonlib.mock.*
import com.github.heheteam.commonlib.statistics.MockTeacherStatistics
import com.github.heheteam.studentbot.StudentCore
import com.github.heheteam.teacherbot.TeacherCore
import org.junit.jupiter.api.BeforeEach

class SolutionDistributionTest {
  private lateinit var coursesDistributor: MockCoursesDistributor
  private lateinit var solutionDistributor: InMemorySolutionDistributor
  private lateinit var userIdRegistry: MockUserIdRegistry
  private lateinit var teacherStatistics: MockTeacherStatistics
  private lateinit var gradeTable: MockGradeTable
  private val problemStorage: ProblemStorage = InMemoryProblemStorage()
  private val assignmentStorage: AssignmentStorage = InMemoryAssignmentStorage()

  private lateinit var teacherCore: TeacherCore

  private lateinit var studentCore: StudentCore

  @BeforeEach
  fun setup() {
    coursesDistributor = MockCoursesDistributor()
    solutionDistributor = InMemorySolutionDistributor()
    teacherStatistics = MockTeacherStatistics()
    gradeTable = MockGradeTable()

    studentCore =
      StudentCore(
        solutionDistributor,
        coursesDistributor,
        problemStorage,
        assignmentStorage,
      )
  }
}
