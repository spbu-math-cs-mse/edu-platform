package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.Teacher
import com.github.heheteam.commonlib.api.ResolveError
import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.api.TeacherStorage
import com.github.heheteam.commonlib.api.toTeacherId
import com.github.heheteam.commonlib.database.tables.ParentStudents
import com.github.heheteam.commonlib.database.tables.StudentTable
import com.github.heheteam.commonlib.database.tables.TeacherTable
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import dev.inmo.tgbotapi.types.UserId
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class DatabaseTeacherStorage(
    val database: Database,
) : TeacherStorage {
    init {
        transaction(database) {
            SchemaUtils.create(StudentTable)
            SchemaUtils.create(ParentStudents)
        }
    }

    override fun createTeacher(
        name: String,
        surname: String,
        tgId: Long,
    ): TeacherId =
        transaction(database) {
            TeacherTable.insert {
                it[TeacherTable.name] = name
                it[TeacherTable.surname] = surname
                it[TeacherTable.tgId] = tgId
            } get TeacherTable.id
        }.value.toTeacherId()

    override fun resolveTeacher(teacherId: TeacherId): Result<Teacher, ResolveError<TeacherId>> =
        transaction(database) {
            val row = TeacherTable
                .selectAll()
                .where(TeacherTable.id eq teacherId.id)
                .singleOrNull() ?: return@transaction Err(ResolveError(teacherId))
            Ok(
                Teacher(
                    teacherId,
                    row[TeacherTable.name],
                    row[TeacherTable.surname],
                ),
            )
        }

    override fun getTeachers(): List<Teacher> =
        transaction(database) {
            TeacherTable.selectAll()
                .map {
                    Teacher(
                        TeacherId(it[TeacherTable.id].value),
                        it[TeacherTable.name],
                        it[TeacherTable.surname],
                    )
                }
        }

    override fun resolveByTgId(tgId: UserId): Result<Teacher, ResolveError<UserId>> =
        transaction(database) {
            val row = TeacherTable
                .selectAll()
                .where(TeacherTable.tgId eq tgId.chatId.long)
                .singleOrNull() ?: return@transaction Err(ResolveError(tgId))
            Ok(
                Teacher(
                    row[TeacherTable.id].value.toTeacherId(),
                    row[TeacherTable.name],
                    row[TeacherTable.surname],
                ),
            )
        }
}
