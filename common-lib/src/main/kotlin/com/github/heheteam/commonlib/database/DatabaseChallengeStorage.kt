package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.Challenge
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.ProblemDescription
import com.github.heheteam.commonlib.database.table.AssignmentTable
import com.github.heheteam.commonlib.database.table.ChallengeTable
import com.github.heheteam.commonlib.database.table.ProblemTable
import com.github.heheteam.commonlib.errors.DatabaseExceptionError
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.heheteam.commonlib.interfaces.AssignmentId
import com.github.heheteam.commonlib.interfaces.ChallengeId
import com.github.heheteam.commonlib.interfaces.ChallengeStorage
import com.github.heheteam.commonlib.interfaces.ChallengingProblemId
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.toChallengeId
import com.github.heheteam.commonlib.interfaces.toChallengingProblemId
import com.github.heheteam.commonlib.util.catchingTransaction
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class DatabaseChallengeStorage(val database: Database) : ChallengeStorage {
  init {
    transaction(database) { SchemaUtils.create(ChallengeTable) }
  }

  override fun createChallenge(
    courseId: CourseId,
    assignmentId: AssignmentId,
    challengeDescription: String,
    challengingProblemsDescriptions: List<ProblemDescription>,
  ): Result<ChallengeId?, DatabaseExceptionError> =
    catchingTransaction(database) {
        if (
          AssignmentTable.selectAll()
            .where { AssignmentTable.id eq assignmentId.long }
            .map { it[AssignmentTable.challengeId] }
            .single() != null
        ) {
          return@catchingTransaction null
        }
        val challengeId =
          ChallengeTable.insertAndGetId {
              it[ChallengeTable.description] = challengeDescription
              it[ChallengeTable.courseId] = courseId.long
              it[ChallengeTable.assignmentId] = assignmentId.long
            }
            .value
            .toChallengeId()
        challengingProblemsDescriptions.mapIndexed { number, problemDescription ->
          createProblem(challengeId, number, problemDescription)
        }
        AssignmentTable.update({ AssignmentTable.id eq assignmentId.long }) {
          it[AssignmentTable.challengeId] = challengeId.long
        }
        challengeId
      }
      .map { it }

  override fun createProblem(
    challengeId: ChallengeId,
    serialNumber: Int,
    problemDescription: ProblemDescription,
  ): Result<ChallengingProblemId, EduPlatformError> =
    catchingTransaction(database) {
        ProblemTable.insertAndGetId {
          it[ProblemTable.serialNumber] = serialNumber
          it[ProblemTable.number] = problemDescription.number
          it[ProblemTable.assignmentId] = challengeId.long
          it[ProblemTable.maxScore] = problemDescription.maxScore
          it[ProblemTable.description] = problemDescription.description
        }
      }
      .map { it.value.toChallengingProblemId() }

  override fun getProblemsWithChallengesFromCourseForStudent(
    courseId: CourseId,
    studentId: StudentId,
  ): Result<Map<Challenge, List<Problem>>, EduPlatformError> {
    TODO("Not yet implemented")
  }
}
