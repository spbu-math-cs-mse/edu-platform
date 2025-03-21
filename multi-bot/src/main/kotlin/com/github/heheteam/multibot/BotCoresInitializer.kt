package com.github.heheteam.multibot

import com.github.heheteam.adminbot.AdminCore
import com.github.heheteam.parentbot.ParentCore
import com.github.heheteam.studentbot.StudentCore
import org.koin.core.module.Module
import org.koin.dsl.module

class BotCoresInitializer {
  fun inject(): Module =
    module {
      single<AdminCore>{AdminCore()}
      single<ParentCore>{ParentCore()}
      single<StudentCore>{StudentCore()}
    }
}
