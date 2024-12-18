package com.github.heheteam.teacherbot

import com.github.heheteam.commonlib.api.*
import com.github.heheteam.commonlib.database.*
import com.github.heheteam.commonlib.facades.CoursesDistributorFacade
import com.github.heheteam.commonlib.facades.GradeTableFacade
import com.github.heheteam.commonlib.googlesheets.GoogleSheetsRatingRecorder
import com.github.heheteam.commonlib.googlesheets.GoogleSheetsService
import com.github.heheteam.commonlib.loadConfig
import com.github.heheteam.commonlib.mock.InMemoryTeacherStatistics
import com.github.heheteam.teacherbot.run.teacherRun
import org.jetbrains.exposed.sql.Database

/**
 * @param args bot token and telegram @username for mocking data.
 */
suspend fun main(vararg args: String) {
    val botToken = args.first()
    val config = loadConfig()

    val database = Database.connect(
        config.databaseConfig.url,
        config.databaseConfig.driver,
        config.databaseConfig.login,
        config.databaseConfig.password,
    )

    val databaseCoursesDistributor = DatabaseCoursesDistributor(database)
    val problemStorage: ProblemStorage = DatabaseProblemStorage(database)
    val assignmentStorage: AssignmentStorage = DatabaseAssignmentStorage(database)
    val solutionDistributor: SolutionDistributor = DatabaseSolutionDistributor(database)
    val databaseGradeTable: GradeTable = DatabaseGradeTable(database)
    val teacherStorage: TeacherStorage = DatabaseTeacherStorage(database)

    val googleSheetsService =
        GoogleSheetsService(config.googleSheetsConfig.serviceAccountKey, config.googleSheetsConfig.spreadsheetId)
    val ratingRecorder = GoogleSheetsRatingRecorder(
        googleSheetsService,
        databaseCoursesDistributor,
        assignmentStorage,
        problemStorage,
        databaseGradeTable,
        solutionDistributor,
    )
    val coursesDistributor = CoursesDistributorFacade(databaseCoursesDistributor, ratingRecorder)
    val gradeTable = GradeTableFacade(databaseGradeTable, ratingRecorder)
    val teacherStatistics = InMemoryTeacherStatistics()
    val botEventBus = RedisBotEventBus()

    val core =
        TeacherCore(
            teacherStatistics,
            coursesDistributor,
            solutionDistributor,
            gradeTable,
            problemStorage,
            botEventBus,
        )

    teacherRun(botToken, teacherStorage, core)
}
