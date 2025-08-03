package com.github.heheteam.studentbot.state.quiz

import com.github.heheteam.commonlib.AttachmentKind
import com.github.heheteam.commonlib.LocalMediaAttachment
import com.github.heheteam.commonlib.api.CommonUserApi
import com.github.heheteam.commonlib.api.ParentApi
import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.interfaces.CommonUserId
import com.github.heheteam.commonlib.interfaces.ParentId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.delete
import com.github.heheteam.studentbot.state.StudentAboutCourseState
import com.github.heheteam.studentbot.state.parent.ParentAboutCourseState
import com.github.michaelbull.result.get
import dev.inmo.kslog.common.error
import dev.inmo.kslog.common.logger
import dev.inmo.tgbotapi.extensions.api.send.media.sendPhoto
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.requests.abstracts.asMultipartFile
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage
import dev.inmo.tgbotapi.utils.row
import java.awt.Color
import java.awt.Font
import java.awt.Font.BOLD
import java.awt.Graphics2D
import java.awt.RenderingHints.KEY_ANTIALIASING
import java.awt.RenderingHints.KEY_RENDERING
import java.awt.RenderingHints.KEY_TEXT_ANTIALIASING
import java.awt.RenderingHints.VALUE_ANTIALIAS_ON
import java.awt.RenderingHints.VALUE_RENDER_QUALITY
import java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO.read
import javax.imageio.ImageIO.write

open class L4Final<ApiService : CommonUserApi<UserId>, UserId : CommonUserId>(
  override val context: User,
  override val userId: UserId,
) : QuestState<ApiService, UserId>() {
  var messageToDelete: AccessibleMessage? = null

  override suspend fun outro(bot: BehaviourContext, service: ApiService) {
    super.outro(bot, service)
    messageToDelete?.let { bot.delete(it) }
  }

  override suspend fun QuestBotContext.run(service: ApiService) {
    saveState(service)
    sendImage("/star.png")
    send("Ты стоишь на вершине. Перед тобой — сияющая звезда.\n")
    send(
      "$DOG_EMOJI Дуся: \"Это — Звезда Дабромат. Она ждала тебя. " +
        "В ней — сила, которая помогает учиться, понимать и придумывать.\""
    )
    val buttons = listOf("\uD83C\uDFC5 Получить Сертификат", "\uD83C\uDF93 Узнать о курсе")
    messageToDelete =
      send(
        text,
        replyMarkup =
          inlineKeyboard {
            row { dataButton(buttons[0], buttons[0]) }
            row { dataButton(buttons[1], buttons[1]) }
          },
      )

    addDataCallbackHandler { callbackQuery ->
      when (callbackQuery.data) {
        buttons[0] -> {
          val userId = userId
          NewState(
            when (userId) {
              is StudentId -> L4CertificateStudent(context, userId)
              is ParentId -> L4CertificateParent(context, userId)
              else -> error("unreachable")
            }
          )
        }
        buttons[1] -> {
          val userId = userId
          NewState(
            when (userId) {
              is StudentId -> StudentAboutCourseState(context, userId)
              is ParentId -> ParentAboutCourseState(context, userId)
              else -> error("unreachable")
            }
          )
        }
        else -> Unhandled
      }
    }
  }

  val text =
    "\uD83D\uDC3E \"Ты герой. А хочешь дальше решать класснее сложные задачи — " +
      "приходи к нам в онлайн-школу Дабромат! " +
      "В учебе я тебя не брошу, не переживай — буду продолжать помогать тебе. " +
      "Но помимо этого ты познакомишься с лучшими преподавателями страны, " +
      "которые прокачают твой мозг так, что ты сможешь стать интеллектуальной элитой, " +
      "зарабатывать горы денег (и подкармливать меня!) и найти много новых друзей, " +
      "столь же увлеченных решением сложных задач, как и ты! \""
}

private const val STUDENT_FONT_SIZE = 48

private const val STUDENT_NAME_Y_POSITION = 800

abstract class L4Certificate<ApiService : CommonUserApi<UserId>, UserId : CommonUserId>(
  override val context: User,
  override val userId: UserId,
) : QuestState<ApiService, UserId>() {
  @Suppress("TooGenericExceptionCaught")
  suspend // should ban it over the whole repo
  fun createCertificateImage(name: String): File? {
    val temporaryFile = File.createTempFile("img", ".png")
    try {
      val templateFile =
        LocalMediaAttachment(AttachmentKind.PHOTO, "/certificate-pure.png").openFile()
      val templateImage: BufferedImage =
        read(templateFile)
          ?: throw IllegalArgumentException(
            "Could not read image from: ${templateFile.canonicalPath}"
          )
      val g2d: Graphics2D = templateImage.createGraphics()
      g2d.setRenderingHint(KEY_TEXT_ANTIALIASING, VALUE_TEXT_ANTIALIAS_ON)
      g2d.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON)
      g2d.setRenderingHint(KEY_RENDERING, VALUE_RENDER_QUALITY)
      g2d.font = Font("Serif", BOLD, STUDENT_FONT_SIZE)
      g2d.color = Color.BLACK
      val fontMetrics = g2d.fontMetrics
      val textWidth = fontMetrics.stringWidth(name)
      val imageWidth = templateImage.width
      val centeredX = (imageWidth - textWidth) / 2
      g2d.drawString(name, centeredX, STUDENT_NAME_Y_POSITION)
      g2d.dispose()
      write(templateImage, "png", temporaryFile)
      return temporaryFile
    } catch (e: Exception) {
      logger.error("Error adding name to certificate: ${e.message}", e)
      return null
    }
  }

  abstract fun getName(service: ApiService): String

  override suspend fun QuestBotContext.run(service: ApiService) {
    saveState(service)
    sendMarkdown("Поздравляем! Ты получаешь *Сертификат Героя Матемаланда* \uD83C\uDFC6\n")
    val name = getName(service)
    val certificate = createCertificateImage(name)
    if (certificate != null) {
      bot.sendPhoto(context, certificate.asMultipartFile())
    } else {
      sendMarkdown("*$name* получает сертификат *Дабромата* за исследование Математлэнда!")
    }
    val buttons = listOf("\uD83C\uDF93 Узнать о курсе\n", "\uD83D\uDD19 В меню!")
    send(
        "$DOG_EMOJI Дуся: \"Покажи его родителям! А я расскажу им, как ты можешь учиться дальше.\"",
        replyMarkup =
          inlineKeyboard {
            row { dataButton(buttons[0], buttons[0]) }
            row { dataButton(buttons[1], buttons[1]) }
          },
      )
      .also { messagesWithKeyboard.add(it) }
    addDataCallbackHandler { callbackQuery ->
      when (callbackQuery.data) {
        buttons[0] -> {
          val userId = this@L4Certificate.userId
          NewState(
            when (userId) {
              is StudentId -> StudentAboutCourseState(context, userId)
              is ParentId -> ParentAboutCourseState(context, userId)
              else -> error("unreachable")
            }
          )
        }
        buttons[1] -> {
          saveState(service)
          NewState(menuState())
        }
        else -> Unhandled
      }
    }
  }
}

class L4FinalStudent(context: User, userId: StudentId) :
  L4Final<StudentApi, StudentId>(context, userId)

class L4FinalParent(context: User, userId: ParentId) :
  L4Final<ParentApi, ParentId>(context, userId)

class L4CertificateStudent(context: User, userId: StudentId) :
  L4Certificate<StudentApi, StudentId>(context, userId) {
  override fun getName(service: StudentApi): String {
    val student = service.loginById(userId).get()
    return if (student != null) {
      student.surname + " " + student.name
    } else {
      "null"
    }
  }
}

class L4CertificateParent(context: User, userId: ParentId) :
  L4Certificate<ParentApi, ParentId>(context, userId) {
  override fun getName(service: ParentApi): String {
    val parent = service.tryLoginByTelegramId(context.id.chatId).get()
    return if (parent != null) {
      parent.lastName + " " + parent.firstName
    } else {
      "null"
    }
  }
}
