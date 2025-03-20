package com.github.heheteam.commonlib

import com.github.heheteam.commonlib.api.AssignmentStorage
import com.github.heheteam.commonlib.api.BotEventBus
import com.github.heheteam.commonlib.api.CoursesDistributor
import com.github.heheteam.commonlib.api.GradeTable
import com.github.heheteam.commonlib.api.ObserverBus
import com.github.heheteam.commonlib.api.ParentStorage
import com.github.heheteam.commonlib.api.ProblemStorage
import com.github.heheteam.commonlib.api.RatingRecorder
import com.github.heheteam.commonlib.api.RedisBotEventBus
import com.github.heheteam.commonlib.api.ResponsibleTeacherResolver
import com.github.heheteam.commonlib.api.ScheduledMessagesDistributor
import com.github.heheteam.commonlib.api.SolutionDistributor
import com.github.heheteam.commonlib.api.StudentStorage
import com.github.heheteam.commonlib.api.TeacherStatistics
import com.github.heheteam.commonlib.api.TeacherStorage
import com.github.heheteam.commonlib.database.DatabaseAssignmentStorage
import com.github.heheteam.commonlib.database.DatabaseCoursesDistributor
import com.github.heheteam.commonlib.database.DatabaseGradeTable
import com.github.heheteam.commonlib.database.DatabaseProblemStorage
import com.github.heheteam.commonlib.database.DatabaseSolutionDistributor
import com.github.heheteam.commonlib.database.DatabaseStudentStorage
import com.github.heheteam.commonlib.database.DatabaseTeacherStorage
import com.github.heheteam.commonlib.database.RandomTeacherResolver
import com.github.heheteam.commonlib.database.table.DatabaseTelegramTechnicalMessagesStorage
import com.github.heheteam.commonlib.database.table.TelegramTechnicalMessagesStorage
import com.github.heheteam.commonlib.decorators.AssignmentStorageDecorator
import com.github.heheteam.commonlib.decorators.CoursesDistributorDecorator
import com.github.heheteam.commonlib.decorators.GradeTableDecorator
import com.github.heheteam.commonlib.decorators.SolutionDistributorDecorator
import com.github.heheteam.commonlib.googlesheets.GoogleSheetsRatingRecorder
import com.github.heheteam.commonlib.googlesheets.GoogleSheetsService
import com.github.heheteam.commonlib.mock.InMemoryScheduledMessagesDistributor
import com.github.heheteam.commonlib.mock.InMemoryTeacherStatistics
import com.github.heheteam.commonlib.mock.MockParentStorage
import org.jetbrains.exposed.sql.Database
import org.koin.core.module.Module
import org.koin.dsl.module

class CoreServicesInitializer {
  fun inject(useRedis: Boolean = false): Module {
    val config = loadConfig()
    val database =
      Database.connect(
        config.databaseConfig.url,
        config.databaseConfig.driver,
        config.databaseConfig.login,
        config.databaseConfig.password,
      )
    return module {
      single<Database> { database }
      val coursesDistributor = DatabaseCoursesDistributor(database)
      val gradeTable = DatabaseGradeTable(database)
      val assignmentStorage = DatabaseAssignmentStorage(database)
      val solutionDistributor = DatabaseSolutionDistributor(database)
      single<ProblemStorage> { DatabaseProblemStorage(database) }
      single<TeacherStorage> { DatabaseTeacherStorage(database) }
      single<TeacherStatistics> { InMemoryTeacherStatistics() }
      single<ScheduledMessagesDistributor> { InMemoryScheduledMessagesDistributor() }
      single<StudentStorage> { DatabaseStudentStorage(database) }
      single<ParentStorage> { MockParentStorage() }

      val googleSheetsService = GoogleSheetsService(config.googleSheetsConfig.serviceAccountKey)
      single<RatingRecorder> {
        GoogleSheetsRatingRecorder(
          googleSheetsService,
          coursesDistributor,
          assignmentStorage,
          problemStorage = get(),
          gradeTable,
          solutionDistributor,
        )
      }

      single<CoursesDistributor> {
        CoursesDistributorDecorator(coursesDistributor, ratingRecorder = get())
      }
      single<GradeTable> { GradeTableDecorator(gradeTable, ratingRecorder = get()) }
      single<AssignmentStorage> {
        AssignmentStorageDecorator(assignmentStorage, ratingRecorder = get())
      }
      single<SolutionDistributor> {
        SolutionDistributorDecorator(solutionDistributor, ratingRecorder = get())
      }

      single<BotEventBus> {
        if (useRedis) RedisBotEventBus(config.redisConfig.host, config.redisConfig.port)
        else ObserverBus()
      }

      single<ResponsibleTeacherResolver> {
        RandomTeacherResolver(problemStorage = get(), assignmentStorage, coursesDistributor)
      }

      single<TelegramTechnicalMessagesStorage> {
        DatabaseTelegramTechnicalMessagesStorage(database)
      }
    }
  }
}
