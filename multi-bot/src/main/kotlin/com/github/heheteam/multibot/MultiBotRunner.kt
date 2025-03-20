package com.github.heheteam.multibot

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.long
import com.github.heheteam.adminbot.run.AdminRunner
import com.github.heheteam.commonlib.CoreServicesInitializer
import com.github.heheteam.commonlib.api.toStudentId
import com.github.heheteam.commonlib.api.toTeacherId
import com.github.heheteam.commonlib.util.DeveloperOptions
import com.github.heheteam.commonlib.util.SampleGenerator
import com.github.heheteam.parentbot.run.ParentRunner
import com.github.heheteam.studentbot.run.StudentRunner
import com.github.heheteam.teacherbot.run.TeacherBotServicesInitializer
import com.github.heheteam.teacherbot.run.TeacherRunner
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.context.GlobalContext.startKoin

class MultiBotRunner : CliktCommand() {
  private val studentBotToken: String by option().required().help("student bot token")
  private val teacherBotToken: String by option().required().help("teacher bot token")
  private val adminBotToken: String by option().required().help("admin bot token")
  private val parentBotToken: String by option().required().help("parent bot token")
  private val presetStudentId: Long? by option().long()
  private val presetTeacherId: Long? by option().long()
  private val useRedis: Boolean by option().boolean().default(false)
  private val needsInit: Boolean by
    option("--init", "-i").flag().help("resets the database and fills it with sample values")

  override fun run() {
    startKoin {
      modules(CoreServicesInitializer().inject(useRedis, true))
      modules(TeacherBotServicesInitializer().inject())
    }

    if (needsInit) SampleGenerator().fillWithSamples()

    val presetStudent = presetStudentId?.toStudentId()
    val presetTeacher = presetTeacherId?.toTeacherId()
    val developerOptions = DeveloperOptions(presetStudent, presetTeacher)

    runBlocking {
      launch { StudentRunner().run(studentBotToken, developerOptions) }
      launch { TeacherRunner().run(teacherBotToken, developerOptions) }
      launch { AdminRunner().run(adminBotToken) }
      launch { ParentRunner().run(parentBotToken) }
    }
  }
}
