package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.AssignmentDependencies
import com.github.heheteam.commonlib.ProblemDescription
import com.github.heheteam.commonlib.database.table.AssessmentTable
import com.github.heheteam.commonlib.database.table.AssignmentTable
import com.github.heheteam.commonlib.database.table.ChallengeAccessTable
import com.github.heheteam.commonlib.database.table.ProblemTable
import com.github.heheteam.commonlib.database.table.SubmissionTable
import com.github.heheteam.commonlib.errors.DatabaseExceptionError
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.heheteam.commonlib.errors.ResolveError
import com.github.heheteam.commonlib.interfaces.AssignmentId
import com.github.heheteam.commonlib.interfaces.AssignmentStorage
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.ProblemStorage
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.toAssignmentId
import com.github.heheteam.commonlib.interfaces.toCourseId
import com.github.heheteam.commonlib.util.catchingTransaction
import com.github.heheteam.commonlib.util.ok
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.FieldSet
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.max
import org.jetbrains.exposed.sql.notExists
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class DatabaseAssignmentStorage(
  val database: Database,
  private val problemStorage: ProblemStorage,
) : AssignmentStorage {
  init {
    transaction(database) { SchemaUtils.createMissingTablesAndColumns(AssignmentTable) }
  }

  override fun resolveAssignment(
    assignmentId: AssignmentId
  ): Result<Assignment, ResolveError<AssignmentId>> {
    return transaction(database) {
      val row =
        AssignmentTable.selectAll().where(AssignmentTable.id eq assignmentId.long).singleOrNull()
          ?: return@transaction Err(ResolveError(assignmentId))
      Ok(
        Assignment(
          assignmentId,
          row[AssignmentTable.serialNumber],
          row[AssignmentTable.description],
          row[AssignmentTable.courseId].value.toCourseId(),
          row[AssignmentTable.statementsUrl],
          row[AssignmentTable.challengeId]?.value?.toAssignmentId(),
        )
      )
    }
  }

  override fun createAssignment(
    courseId: CourseId,
    description: String,
    statementsUrl: String?,
    problemsDescriptions: List<ProblemDescription>,
  ): Result<AssignmentId, DatabaseExceptionError> =
    catchingTransaction(database) {
      val serialNumber =
        (AssignmentTable.select(AssignmentTable.serialNumber.max())
          .where(AssignmentTable.courseId eq courseId.long)
          .firstOrNull()
          ?.get(AssignmentTable.serialNumber.max()) ?: 0) + 1
      val assignId =
        AssignmentTable.insertAndGetId {
            it[AssignmentTable.serialNumber] = serialNumber
            it[AssignmentTable.description] = description
            it[AssignmentTable.courseId] = courseId.long
            it[AssignmentTable.statementsUrl] = statementsUrl
            it[AssignmentTable.challengeId] = null
            it[AssignmentTable.isChallenge] = false
          }
          .value
          .toAssignmentId()
      problemsDescriptions.mapIndexed { number, problemDescription ->
        problemStorage.createProblem(assignId, number, problemDescription)
      }

      assignId
    }

  private fun FieldSet.countDependencies(assignment: Assignment) =
    this.selectAll()
      .where {
        ((ProblemTable.assignmentId eq assignment.id.long) or
          (ProblemTable.assignmentId eq assignment.challengeId?.long))
      }
      .count()

  override fun resolveAssignmentAndDependencies(
    assignmentId: AssignmentId
  ): Result<AssignmentDependencies, EduPlatformError> =
    catchingTransaction(database) {
      val assignment = resolveAssignment(assignmentId).value
      val numberOfDependentProblems = ProblemTable.countDependencies(assignment)
      val numberOfDependentSubmissions =
        ProblemTable.join(
            SubmissionTable,
            joinType = JoinType.INNER,
            onColumn = ProblemTable.id,
            otherColumn = SubmissionTable.problemId,
          )
          .countDependencies(assignment)
      val numberOfDependentAssessments =
        ProblemTable.join(
            SubmissionTable,
            joinType = JoinType.INNER,
            onColumn = ProblemTable.id,
            otherColumn = SubmissionTable.problemId,
          )
          .join(
            AssessmentTable,
            joinType = JoinType.INNER,
            onColumn = SubmissionTable.id,
            otherColumn = AssessmentTable.submissionId,
          )
          .countDependencies(assignment)

      AssignmentDependencies(
        assignment,
        numberOfDependentProblems,
        numberOfDependentSubmissions,
        numberOfDependentAssessments,
      )
    }

  override fun deleteAssignment(assignmentId: AssignmentId): Result<Unit, DatabaseExceptionError> =
    catchingTransaction(database) {
      val challengeId =
        AssignmentTable.selectAll()
          .where { AssignmentTable.id eq assignmentId.long }
          .singleOrNull()
          ?.get(AssignmentTable.challengeId)
          ?.value
      if (challengeId == null) {
        AssignmentTable.deleteWhere { AssignmentTable.id eq assignmentId.long }
      } else {
        AssignmentTable.deleteWhere {
          (AssignmentTable.id eq assignmentId.long) or (AssignmentTable.id eq challengeId)
        }
      }
    }

  override fun createChallenge(
    assignmentId: AssignmentId,
    courseId: CourseId,
    description: String,
    statementsUrl: String?,
    problemsDescriptions: List<ProblemDescription>,
  ): Result<AssignmentId, DatabaseExceptionError> =
    catchingTransaction(database) {
      val serialNumber =
        (AssignmentTable.select(AssignmentTable.serialNumber.max())
          .where(AssignmentTable.courseId eq courseId.long)
          .firstOrNull()
          ?.get(AssignmentTable.serialNumber.max()) ?: 0) + 1
      val assignId =
        AssignmentTable.insertAndGetId {
            it[AssignmentTable.serialNumber] = serialNumber
            it[AssignmentTable.description] = description
            it[AssignmentTable.courseId] = courseId.long
            it[AssignmentTable.statementsUrl] = statementsUrl
            it[AssignmentTable.challengeId] = null
            it[AssignmentTable.isChallenge] = true
          }
          .value
          .toAssignmentId()
      problemsDescriptions.mapIndexed { number, problemDescription ->
        problemStorage.createProblem(assignId, number, problemDescription)
      }

      assignId
    }

  override fun grantAccessToChallenge(
    studentId: StudentId,
    courseId: CourseId,
  ): Result<Unit, DatabaseExceptionError> =
    catchingTransaction(database) {
      val challenges =
        AssignmentTable.select(AssignmentTable.id).where {
          (AssignmentTable.isChallenge eq true) and
            (AssignmentTable.courseId eq courseId.long) and
            (notExists(
              ChallengeAccessTable.selectAll().where {
                (ChallengeAccessTable.challengeId eq AssignmentTable.id) and
                  (ChallengeAccessTable.studentId eq studentId.long)
              }
            ))
        }
      ChallengeAccessTable.batchInsert(challenges) {
        this[ChallengeAccessTable.studentId] = studentId.long
        this[ChallengeAccessTable.challengeId] = it[AssignmentTable.id]
      }
    }

  override fun getAssignmentsForCourse(
    courseId: CourseId
  ): Result<List<Assignment>, EduPlatformError> =
    transaction {
        AssignmentTable.selectAll().where(AssignmentTable.courseId eq courseId.long).map {
          Assignment(
            it[AssignmentTable.id].value.toAssignmentId(),
            it[AssignmentTable.serialNumber],
            it[AssignmentTable.description],
            it[AssignmentTable.courseId].value.toCourseId(),
            it[AssignmentTable.statementsUrl],
            it[AssignmentTable.challengeId]?.value?.toAssignmentId(),
          )
        }
      }
      .ok()
}
