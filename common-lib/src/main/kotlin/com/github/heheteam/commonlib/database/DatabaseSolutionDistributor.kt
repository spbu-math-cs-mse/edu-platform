package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.ResolveError
import com.github.heheteam.commonlib.Solution
import com.github.heheteam.commonlib.SolutionContent
import com.github.heheteam.commonlib.SolutionInputRequest
import com.github.heheteam.commonlib.SolutionResolveError
import com.github.heheteam.commonlib.TeacherDoesNotExist
import com.github.heheteam.commonlib.database.table.AssessmentTable
import com.github.heheteam.commonlib.database.table.AssignmentTable
import com.github.heheteam.commonlib.database.table.CourseTable
import com.github.heheteam.commonlib.database.table.CourseTeachers
import com.github.heheteam.commonlib.database.table.ProblemTable
import com.github.heheteam.commonlib.database.table.SolutionTable
import com.github.heheteam.commonlib.database.table.TeacherTable
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.ProblemId
import com.github.heheteam.commonlib.interfaces.SolutionDistributor
import com.github.heheteam.commonlib.interfaces.SolutionId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.heheteam.commonlib.interfaces.toSolutionId
import com.github.heheteam.commonlib.interfaces.toTeacherId
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.toChatId
import java.time.LocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class DatabaseSolutionDistributor(val database: Database) : SolutionDistributor {
  override fun inputSolution(
    studentId: StudentId,
    chatId: RawChatId,
    messageId: MessageId,
    solutionContent: SolutionContent,
    problemId: ProblemId,
    timestamp: LocalDateTime,
    teacherId: TeacherId?,
  ): SolutionId {
    val solutionId =
      transaction(database) {
          SolutionTable.insert {
            it[SolutionTable.studentId] = studentId.long
            it[SolutionTable.chatId] = chatId.toChatId().chatId.long
            it[SolutionTable.messageId] = messageId.long
            it[SolutionTable.problemId] = problemId.long
            it[SolutionTable.timestamp] = timestamp.toKotlinLocalDateTime()
            it[SolutionTable.solutionContent] = solutionContent
            it[SolutionTable.responsibleTeacher] = teacherId?.long
          } get SolutionTable.id
        }
        .value
    return SolutionId(solutionId)
  }

  override fun querySolution(teacherId: TeacherId): Result<Solution?, SolutionResolveError> =
    transaction(database) {
      val teacherRow =
        TeacherTable.select(TeacherTable.id).where(TeacherTable.id eq teacherId.long).firstOrNull()
          ?: return@transaction Err(TeacherDoesNotExist(teacherId))
      val courses =
        CourseTeachers.select(CourseTeachers.courseId)
          .where(CourseTeachers.teacherId eq teacherRow[TeacherTable.id])
          .map { course -> course[CourseTeachers.courseId] }

      val solution =
        SolutionTable.join(
            AssessmentTable,
            JoinType.LEFT,
            onColumn = SolutionTable.id,
            otherColumn = AssessmentTable.solutionId,
          )
          .join(
            ProblemTable,
            JoinType.INNER,
            onColumn = SolutionTable.problemId,
            otherColumn = ProblemTable.id,
          )
          .join(
            AssignmentTable,
            JoinType.INNER,
            onColumn = ProblemTable.assignmentId,
            otherColumn = AssignmentTable.id,
          )
          .join(
            CourseTable,
            JoinType.INNER,
            onColumn = AssignmentTable.courseId,
            otherColumn = CourseTable.id,
          )
          .selectAll()
          .where {
            AssessmentTable.id.isNull() and
              (CourseTable.id inList courses) and
              (SolutionTable.responsibleTeacher eq teacherId.long)
          }
          .orderBy(SolutionTable.timestamp)
          .firstOrNull() ?: return@transaction Ok(null)

      Ok(
        Solution(
          solution[SolutionTable.id].value.toSolutionId(),
          StudentId(solution[SolutionTable.studentId].value),
          solution[SolutionTable.chatId].toChatId().chatId,
          MessageId(solution[SolutionTable.messageId]),
          ProblemId(solution[SolutionTable.problemId].value),
          solution[SolutionTable.solutionContent],
          solution[SolutionTable.responsibleTeacher]?.value?.toTeacherId(),
          solution[SolutionTable.timestamp],
        )
      )
    }

  @Suppress("LongMethod") // a long database query
  override fun querySolution(courseId: CourseId): Result<Solution?, SolutionResolveError> =
    transaction(database) {
      val solution =
        SolutionTable.join(
            AssessmentTable,
            JoinType.LEFT,
            onColumn = SolutionTable.id,
            otherColumn = AssessmentTable.solutionId,
          )
          .join(
            ProblemTable,
            JoinType.INNER,
            onColumn = SolutionTable.problemId,
            otherColumn = ProblemTable.id,
          )
          .join(
            AssignmentTable,
            JoinType.INNER,
            onColumn = ProblemTable.assignmentId,
            otherColumn = AssignmentTable.id,
          )
          .join(
            CourseTable,
            JoinType.INNER,
            onColumn = AssignmentTable.courseId,
            otherColumn = CourseTable.id,
          )
          .selectAll()
          .where { AssessmentTable.id.isNull() and (CourseTable.id eq courseId.long) }
          .orderBy(SolutionTable.timestamp)
          .firstOrNull() ?: return@transaction Ok(null)

      Ok(
        Solution(
          solution[SolutionTable.id].value.toSolutionId(),
          StudentId(solution[SolutionTable.studentId].value),
          solution[SolutionTable.chatId].toChatId().chatId,
          MessageId(solution[SolutionTable.messageId]),
          ProblemId(solution[SolutionTable.problemId].value),
          solution[SolutionTable.solutionContent],
          solution[SolutionTable.responsibleTeacher]?.value?.toTeacherId(),
          solution[SolutionTable.timestamp],
        )
      )
    }

  override fun resolveSolution(solutionId: SolutionId): Result<Solution, ResolveError<SolutionId>> =
    transaction(database) {
      val solution =
        SolutionTable.selectAll().where { SolutionTable.id eq solutionId.long }.singleOrNull()
          ?: return@transaction Err(ResolveError(solutionId))

      Ok(
        Solution(
          solutionId,
          StudentId(solution[SolutionTable.studentId].value),
          solution[SolutionTable.chatId].toChatId().chatId,
          MessageId(solution[SolutionTable.messageId]),
          ProblemId(solution[SolutionTable.problemId].value),
          solution[SolutionTable.solutionContent],
          solution[SolutionTable.responsibleTeacher]?.value?.toTeacherId(),
          solution[SolutionTable.timestamp],
        )
      )
    }

  override fun resolveSolutionCourse(
    solutionId: SolutionId
  ): Result<CourseId, ResolveError<SolutionId>> =
    transaction(database) {
      SolutionTable.join(
          ProblemTable,
          JoinType.INNER,
          onColumn = SolutionTable.problemId,
          otherColumn = ProblemTable.id,
        )
        .join(
          AssignmentTable,
          JoinType.INNER,
          onColumn = ProblemTable.assignmentId,
          otherColumn = AssignmentTable.id,
        )
        .selectAll()
        .where { SolutionTable.id eq solutionId.long }
        .singleOrNull()
        ?.let { Ok(CourseId(it[AssignmentTable.courseId].value)) }
        ?: return@transaction Err(ResolveError(solutionId))
    }

  override fun resolveResponsibleTeacher(solution: SolutionInputRequest): TeacherId? =
    transaction(database) {
      SolutionTable.selectAll()
        .where {
          (SolutionTable.problemId eq solution.problemId.long) and
            (SolutionTable.studentId eq solution.studentId.long)
        }
        .map { row -> row[SolutionTable.responsibleTeacher]?.value?.toTeacherId() }
        .firstOrNull()
    }

  override fun getSolutionsForProblem(problemId: ProblemId): List<SolutionId> =
    transaction(database) {
      SolutionTable.selectAll()
        .where { SolutionTable.problemId eq problemId.long }
        .map { row -> SolutionId(row[SolutionTable.id].value) }
    }

  override fun isSolutionAssessed(solutionId: SolutionId): Boolean =
    transaction(database) {
      AssessmentTable.select(AssessmentTable.id)
        .where { AssessmentTable.solutionId eq solutionId.long }
        .firstOrNull() != null
    }
}
