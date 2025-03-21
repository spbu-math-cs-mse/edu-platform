package com.github.heheteam.teacherbot.run

import com.github.heheteam.teacherbot.logic.JournalUpdater
import com.github.heheteam.teacherbot.logic.NewSolutionTeacherNotifier
import com.github.heheteam.teacherbot.logic.PrettyTechnicalMessageService
import com.github.heheteam.teacherbot.logic.SolutionCourseResolver
import com.github.heheteam.teacherbot.logic.SolutionCourseResolverImpl
import com.github.heheteam.teacherbot.logic.SolutionGrader
import com.github.heheteam.teacherbot.logic.StudentNewGradeNotifier
import com.github.heheteam.teacherbot.logic.StudentNewGradeNotifierImpl
import com.github.heheteam.teacherbot.logic.TechnicalMessageUpdater
import com.github.heheteam.teacherbot.logic.TechnicalMessageUpdaterImpl
import com.github.heheteam.teacherbot.logic.TelegramBotControllersRepository
import com.github.heheteam.teacherbot.logic.TelegramMessagesJournalUpdater
import com.github.heheteam.teacherbot.logic.TelegramSolutionSender
import com.github.heheteam.teacherbot.logic.TelegramSolutionSenderImpl
import com.github.heheteam.teacherbot.logic.UiController
import com.github.heheteam.teacherbot.logic.UiControllerTelegramSender
import org.koin.core.component.KoinComponent
import org.koin.dsl.module

class TeacherBotServicesInitializer : KoinComponent {
  private val botControllers = TelegramBotControllersRepository()

  fun inject() = module {
    single<PrettyTechnicalMessageService>{ PrettyTechnicalMessageService() }
    single<JournalUpdater> { TelegramMessagesJournalUpdater() }
    single<StudentNewGradeNotifier> { StudentNewGradeNotifierImpl() }
    single<UiController> { UiControllerTelegramSender() }
    single<SolutionGrader> { SolutionGrader() }

    val telegramSolutionSender = TelegramSolutionSenderImpl().also { botControllers.add(it) }
    single<TelegramSolutionSender> { telegramSolutionSender }

    single<SolutionCourseResolver> { SolutionCourseResolverImpl() }
    single<NewSolutionTeacherNotifier> { NewSolutionTeacherNotifier() }

    val technicalMessageUpdaterImpl = TechnicalMessageUpdaterImpl().also { botControllers.add(it) }
    single<TechnicalMessageUpdater> { technicalMessageUpdaterImpl }
    single<TelegramBotControllersRepository> { botControllers }
  }
}
