package com.github.heheteam.studentbot.run

import com.github.heheteam.studentbot.logic.NotificationService
import com.github.heheteam.studentbot.logic.StudentNotificationService
import com.github.heheteam.studentbot.logic.TelegramBotControllersRepository
import org.koin.core.component.KoinComponent
import org.koin.dsl.module

class StudentBotServicesInitializer : KoinComponent {
  private val botControllers = TelegramBotControllersRepository()

  fun inject() = module {
    val notificationService = StudentNotificationService().also { botControllers.add(it) }
    single<NotificationService> { notificationService }
    single<TelegramBotControllersRepository> { botControllers }
  }
}
