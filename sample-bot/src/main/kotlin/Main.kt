package com.github.heheteam.samplebot
// test
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.send.media.sendPhoto
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitAnyContentMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndFSMAndStartLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.command
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.replyKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.simpleButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.urlButton
import dev.inmo.tgbotapi.requests.abstracts.InputFile
import dev.inmo.tgbotapi.types.IdChatIdentifier
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.ReplyKeyboardRemove
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.message.textsources.botCommand
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.row
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first

class Answer(
  val answer: String,
  val correct: Boolean = false,
)

class Question(
  val image: String,
  val question: String,
  val answers: List<Answer>,
  val wiki: String,
)

sealed interface BotState : State

data class StartState(
  override val context: IdChatIdentifier,
) : BotState

data class MenuState(
  override val context: IdChatIdentifier,
) : BotState

data class QuizSolvingState(
  override val context: IdChatIdentifier,
) : BotState

data class SolutionSendingState(
  override val context: IdChatIdentifier,
) : BotState

val question =
  Question(
    image = "https://upload.wikimedia.org/wikipedia/commons/a/a5/Tsunami_by_hokusai_19th_century.jpg",
    question = "Who painted this?",
    answers =
    listOf(
      Answer("Hokusai", correct = true),
      Answer("Sukenobu"),
      Answer("Chōshun"),
      Answer("Kiyonobu I"),
    ),
    wiki = "https://en.wikipedia.org/wiki/Ukiyo-e",
  )

suspend fun main(vararg args: String) {
  val botToken = args.first()
  val bot =
    telegramBot(botToken) {
      logger =
        KSLog { level: LogLevel, tag: String?, message: Any, throwable: Throwable? ->
          println(defaultMessageFormatter(level, tag, message, throwable))
        }
    }

  telegramBotWithBehaviourAndFSMAndStartLongPolling<BotState>(
    botToken,
    CoroutineScope(Dispatchers.IO),
    onStateHandlingErrorHandler = { state, e ->
      println("Thrown error on $state")
      e.printStackTrace()
      state
    },
  ) {
    println(getMe())

    command(
      "start",
    ) {
      startChain(StartState(it.chat.id))
    }

    strictlyOn<StartState> { state ->
      bot.send(
        state.context,
        "Click these weeny buttons:",
        replyMarkup =
        InlineKeyboardMarkup(
          keyboard =
          matrix {
            row {
              dataButton("Quiz", "quiz")
              urlButton("Silly URL", "https://xkcd.com/${(1..3000).random()}/")
              dataButton("Emoji \uD83D\uDE15", "emoji \uD83D\uDE15")
            }
            row {
              urlButton("Library", "https://github.com/InsanusMokrassar/ktgbotapi")
              urlButton("Docs", "https://docs.inmo.dev/tgbotapi/index.html")
            }
            row {
              dataButton("Send me sticker pls", "send me sticker")
              dataButton("Send photo", "send photo")
            }
          },
        ),
      )

      MenuState(state.context)
    }

    strictlyOn<MenuState> { state ->
      val callback = waitDataCallbackQuery().first()
      val data = callback.data
      when {
        data == "quiz" -> {
          bot.sendPhoto(
            chatId = state.context,
            fileId = InputFile.fromUrl(question.image),
            text = question.question,
            replyMarkup =
            replyKeyboard {
              for (answer in question.answers.shuffled()) {
                row {
                  simpleButton(
                    text = answer.answer,
                  )
                }
              }
            },
          )
          QuizSolvingState(state.context)
        }

        data == "emoji \uD83D\uDE15" -> {
          bot.sendTextMessage(
            chatId = state.context,
            text = "\uD83D\uDE15",
          )
          StartState(state.context)
        }

        data == "send me sticker" -> {
          bot.sendSticker(
            chatId = state.context,
            sticker = InputFile.fromId("CAACAgEAAxkBAAIBJWcUPyqe-UEVGqMmhNYi21U3gkBEAALrAQACOA6CEbOGBM7hnEk5NgQ"),
          )
          StartState(state.context)
        }

        data == "send photo" -> {
          SolutionSendingState(state.context)
        }

        else -> MenuState(state.context)
      }
    }

    strictlyOn<SolutionSendingState> { state ->
      send(
        state.context,
      ) {
        +"Send me solution or " + botCommand("stop") + " if you change your mind"
      }

      val contentMessage = waitAnyContentMessage().first()
      val content = contentMessage.content

      when {
        content is TextContent && content.text == "/stop" ->
          StartState(state.context)

        else -> {
          execute(content.createResend(state.context))
          state
        }
      }
    }

    strictlyOn<QuizSolvingState> { state ->
      val message = waitTextMessage().first()
      val answer = message.content.text
      when {
        answer == "/stop" -> MenuState(state.context)

        else -> {
          val nextState: BotState

          if (question.answers.find { it.answer == answer }?.correct == true) {
            bot.reply(
              message,
              text = "✅ \"$answer\" is a correct answer!",
              replyMarkup = ReplyKeyboardRemove(),
            )
            nextState = StartState(state.context)
          } else {
            bot.reply(
              message,
              text = "❌ Try again, \"$answer\" is not a correct answer…",
            )
            nextState = QuizSolvingState(state.context)
          }

          return@strictlyOn nextState
        }
      }
    }

    allUpdatesFlow.subscribeSafelyWithoutExceptions(this) {
      println(it)
    }
  }.second.join()
}
