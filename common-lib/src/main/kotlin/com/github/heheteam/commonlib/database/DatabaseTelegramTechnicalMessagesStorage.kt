package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.api.SolutionDistributor
import com.github.heheteam.commonlib.api.SolutionId
import com.github.heheteam.commonlib.api.TelegramMessageInfo
import com.github.heheteam.commonlib.api.TelegramTechnicalMessagesStorage
import com.github.heheteam.commonlib.api.toTeacherId
import com.github.heheteam.commonlib.database.table.SolutionGroupMessagesTable
import com.github.heheteam.commonlib.database.table.SolutionPersonalMessagesTable
import com.github.heheteam.commonlib.database.table.SolutionTable
import com.github.heheteam.commonlib.database.table.TeacherMenuMessageTable
import com.github.heheteam.commonlib.database.table.TeacherTable
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.get
import com.github.michaelbull.result.toResultOr
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.error
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.longLiteral
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.upsert
import org.jetbrains.exposed.sql.alias

class DatabaseTelegramTechnicalMessagesStorage(
    val database: Database,
    val solutionDistributor: SolutionDistributor,
) :
    TelegramTechnicalMessagesStorage {
    init {
        transaction(database) {
            SchemaUtils.create(SolutionGroupMessagesTable)
            SchemaUtils.create(SolutionPersonalMessagesTable)
            SchemaUtils.create(TeacherMenuMessageTable)
        }
    }

    override fun registerGroupSolutionPublication(
        solutionId: SolutionId,
        telegramMessageInfo: TelegramMessageInfo,
    ) {
        transaction(database) {
            SolutionGroupMessagesTable.insert {
                it[SolutionGroupMessagesTable.solutionId] = solutionId.id
                it[messageId] = telegramMessageInfo.messageId.long
                it[chatId] = telegramMessageInfo.chatId.long
            }
        }
    }

    override fun registerPersonalSolutionPublication(
        solutionId: SolutionId,
        telegramMessageInfo: TelegramMessageInfo,
    ) {
        transaction(database) {
            SolutionPersonalMessagesTable.insert {
                it[SolutionPersonalMessagesTable.solutionId] = solutionId.id
                it[messageId] = telegramMessageInfo.messageId.long
                it[chatId] = telegramMessageInfo.chatId.long
            }
        }
    }

    override fun resolveGroupMessage(solutionId: SolutionId): Result<TelegramMessageInfo, String> {
        val row =
            transaction(database) {
                SolutionGroupMessagesTable.selectAll()
                    .where(SolutionGroupMessagesTable.solutionId eq solutionId.id)
                    .map {
                        TelegramMessageInfo(
                            RawChatId(it[SolutionGroupMessagesTable.chatId]),
                            MessageId(it[SolutionGroupMessagesTable.messageId]),
                        )
                    }
            }
        return row.singleOrNull().toResultOr { "" }
    }

    override fun resolvePersonalMessage(solutionId: SolutionId): Result<TelegramMessageInfo, String> {
        val row =
            transaction(database) {
                SolutionPersonalMessagesTable.selectAll()
                    .where(SolutionPersonalMessagesTable.solutionId eq solutionId.id)
                    .map {
                        TelegramMessageInfo(
                            RawChatId(it[SolutionPersonalMessagesTable.chatId]),
                            MessageId(it[SolutionPersonalMessagesTable.messageId]),
                        )
                    }
            }
        return row.singleOrNull().toResultOr { "" }
    }

    override fun updateTeacherMenuMessage(telegramMessageInfo: TelegramMessageInfo) {
        transaction(database) {
            try {
                TeacherMenuMessageTable.upsert(
                    TeacherMenuMessageTable.id,
                    onUpdate = mutableListOf(
                        TeacherMenuMessageTable.messageId to longLiteral(telegramMessageInfo.messageId.long),
                        TeacherMenuMessageTable.chatId to longLiteral(telegramMessageInfo.chatId.long),
                    )
                )
                {
                    it[messageId] = telegramMessageInfo.messageId.long
                    it[chatId] = telegramMessageInfo.chatId.long
                }
            } catch (e: IllegalStateException) {
                KSLog.error(e)
                TeacherMenuMessageTable.insert {
                    it[messageId] = telegramMessageInfo.messageId.long
                    it[chatId] = telegramMessageInfo.chatId.long
                }
            }

        }
    }

    override fun resolveTeacherMenuMessage(solutionId: SolutionId): Result<TelegramMessageInfo, String> {
        val row =
            transaction(database) {
                SolutionPersonalMessagesTable
                    .join(
                        TeacherMenuMessageTable,
                        joinType = JoinType.INNER,
                        onColumn = SolutionPersonalMessagesTable.chatId,
                        otherColumn = TeacherMenuMessageTable.chatId,
                    )
                    .selectAll()
                    .where(SolutionPersonalMessagesTable.solutionId eq solutionId.id)
                    .map {
                        TelegramMessageInfo(
                            RawChatId(it[TeacherMenuMessageTable.chatId]),
                            MessageId(it[TeacherMenuMessageTable.messageId]),
                        )
                    }
            }
        return row.singleOrNull().toResultOr { "" }
    }

    override fun resolveTeacherFirstUncheckedSolutionMessage(
        solutionId: SolutionId,
    ): Result<TelegramMessageInfo, String> {
        val row =
            transaction(database) {
                val teacherId = SolutionPersonalMessagesTable
                    .join(
                        TeacherTable,
                        joinType = JoinType.INNER,
                        onColumn = SolutionPersonalMessagesTable.chatId,
                        otherColumn = TeacherTable.tgId
                    )
                    .select(TeacherTable.id)
                    .where(SolutionPersonalMessagesTable.solutionId eq solutionId.id)
                    .map { it[TeacherTable.id].value.toTeacherId() }.firstOrNull() ?: return@transaction null
                val solution = solutionDistributor.querySolution(teacherId).get() ?: return@transaction null
                return@transaction SolutionPersonalMessagesTable
                    .selectAll()
                    .where(SolutionPersonalMessagesTable.solutionId eq solution.id.id)
                    .map {
                        TelegramMessageInfo(
                            RawChatId(it[SolutionPersonalMessagesTable.chatId]),
                            MessageId(it[SolutionPersonalMessagesTable.messageId]),
                        )
                    }
            }
        return row?.singleOrNull().toResultOr { "" }
    }
}
