package com.github.heheteam.studentbot.state.quiz

import com.github.heheteam.commonlib.api.CommonUserApi
import com.github.heheteam.commonlib.api.ParentApi
import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.interfaces.CommonUserId
import com.github.heheteam.commonlib.interfaces.ParentId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.studentbot.state.StudentAboutCourseState
import com.github.heheteam.studentbot.state.parent.ParentAboutCourseState
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.urlButton
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.utils.row

private const val DABROMAT_URL = "https://dabromat.ru/start"

open class L4Final<ApiService : CommonUserApi<UserId>, UserId : CommonUserId>(
  override val context: User,
  override val userId: UserId,
) : QuestState<ApiService, UserId>() {
  override suspend fun BotContext.run(service: ApiService) {
    saveState(service)
    sendImage("/star.png")
    send("Ты стоишь на вершине. Перед тобой — сияющая звезда.\n")
    send(
      "$DOG_EMOJI Дуся: \"Это — Звезда Дабромат. Она ждала тебя. " +
        "В ней — сила, которая помогает учиться, понимать и придумывать.\""
    )
    val buttons = listOf("\uD83C\uDFC5 Получить Сертификат", "\uD83C\uDF93 Узнать о курсе")
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

open class L4Certificate<ApiService : CommonUserApi<UserId>, UserId : CommonUserId>(
  override val context: User,
  override val userId: UserId,
) : QuestState<ApiService, UserId>() {
  override suspend fun BotContext.run(service: ApiService) {
    saveState(service)
    sendMarkdown("Поздравляем! Ты получаешь *Сертификат Героя Матемаланда* \uD83C\uDFC6\n")
    val buttons = listOf("\uD83C\uDF93 Узнать о курсе\n", "\uD83D\uDD19 В меню!")
    send(
        "$DOG_EMOJI Дуся: \"Покажи его родителям! А я расскажу им, как ты можешь учиться дальше.\"",
        replyMarkup =
          inlineKeyboard {
            row { urlButton(buttons[0], buttons[0]) }
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
  L4Certificate<StudentApi, StudentId>(context, userId)

class L4CertificateParent(context: User, userId: ParentId) :
  L4Certificate<ParentApi, ParentId>(context, userId)
