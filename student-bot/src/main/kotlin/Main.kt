package com.github.heheteam.studentbot

import DatabaseCoursesDistributor
import com.github.heheteam.commonlib.api.AssignmentStorage
import com.github.heheteam.commonlib.api.ProblemStorage
import com.github.heheteam.commonlib.database.*
import com.github.heheteam.commonlib.mock.*
import com.github.heheteam.commonlib.util.fillWithSamples
import com.github.heheteam.studentbot.run.studentRun
import dev.inmo.tgbotapi.utils.RiskFeature
import org.jetbrains.exposed.sql.Database

@OptIn(RiskFeature::class)
suspend fun main(vararg args: String) {
  val botToken = args.first()

  val database = Database.connect(
    "jdbc:h2:./data/films",
    driver = "org.h2.Driver",
  )

  val studentStorage = DatabaseStudentStorage(database)
  val coursesDistributor = DatabaseCoursesDistributor(database)
  val problemStorage: ProblemStorage = DatabaseProblemStorage(database)
  val assignmentStorage: AssignmentStorage = DatabaseAssignmentStorage(database)
  val solutionDistributor = DatabaseSolutionDistributor(database)

  fillWithSamples(coursesDistributor, problemStorage, assignmentStorage, studentStorage)

  val userIdRegistry = MockStudentIdRegistry(1L)

  val core =
    StudentCore(
      solutionDistributor,
      coursesDistributor,
      problemStorage,
      assignmentStorage,
      DatabaseGradeTable(database),
    )

  studentRun(botToken, userIdRegistry, core)
}
