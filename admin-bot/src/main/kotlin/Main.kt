package com.github.heheteam.adminbot

import Course
import Student
import Teacher
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndFSMAndStartLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.command
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import dev.inmo.tgbotapi.extensions.utils.types.buttons.*
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.ReplyKeyboardRemove
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.textsources.botCommand
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.newLine
import dev.inmo.tgbotapi.utils.row
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first

sealed interface BotState : State

data class StartState(
  override val context: User,
) : BotState

data class NotAdminState(
  override val context: User,
) : BotState

data class MenuState(
  override val context: User,
) : BotState

data class CreateCourseState(
  override val context: User,
) : BotState

data class PickACourseState(
  override val context: User,
) : BotState

data class EditCourseState(
  override val context: User,
  val course: Course,
  val courseName: String,
) : BotState

data class AddStudentState(
  override val context: User,
  val course: Course,
  val courseName: String,
) : BotState

data class RemoveStudentState(
  override val context: User,
  val course: Course,
  val courseName: String,
) : BotState

data class AddTeacherState(
  override val context: User,
  val course: Course,
  val courseName: String,
) : BotState

data class RemoveTeacherState(
  override val context: User,
  val course: Course,
  val courseName: String,
) : BotState

data class EditDescriptionState(
  override val context: User,
  val course: Course,
  val courseName: String,
) : BotState

suspend fun main(vararg args: String) {
  val botToken = args.first()
  mockTgUsername = args[1]
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
      startChain(StartState(it.from!!))
    }

    strictlyOn<StartState> { state ->
      if (state.context.username == null) {
        return@strictlyOn null
      }
      val username = state.context.username!!.username
      if (mockAdmins.containsValue(username)) {
        bot.send(
          state.context,
          "Главное меню:",
          replyMarkup =
          InlineKeyboardMarkup(
            keyboard =
            matrix {
              row {
                dataButton("Создать курс", "create course")
              }
              row {
                dataButton("Изменить курс", "edit course")
              }
            },
          ),
        )
        MenuState(state.context)
      } else {
        send(
          state.context,

          "У вас нет прав администратора",
          replyMarkup = InlineKeyboardMarkup(
            keyboard = matrix {
              row {
                dataButton("Проверить ещё раз", "update")
              }
            },
          ),
        )
        NotAdminState(state.context)
      }
    }

    strictlyOn<NotAdminState> { state ->
      val callback = waitDataCallbackQuery().first()
      val data = callback.data
      when {
        data == "update" ->
          StartState(state.context)

        else -> NotAdminState(state.context)
      }
    }

    strictlyOn<MenuState> { state ->
      val callback = waitDataCallbackQuery().first()
      val data = callback.data
      answerCallbackQuery(callback)
      when {
        data == "create course" -> {
          send(
            state.context,
          ) {
            +"Введите название курса, который хотите создать, или " + botCommand("stop") + " чтобы отменить операцию"
          }
          CreateCourseState(state.context)
        }

        data == "edit course" -> {
          bot.send(
            state.context,
            "Выберите курс, который хотите изменить:",
            replyMarkup =
            replyKeyboard {
              for ((name, _) in mockCourses) {
                row {
                  simpleButton(
                    text = name,
                  )
                }
              }
            },
          )
          PickACourseState(state.context)
        }

        else -> MenuState(state.context)
      }
    }

    strictlyOn<CreateCourseState> { state ->
      val message = waitTextMessage().first()
      val answer = message.content.text

      when {
        answer == "/stop" ->
          StartState(state.context)

        mockCourses.containsKey(answer) -> {
          send(
            state.context,
          ) {
            +"Курс с таким названием уже существует"
          }
          CreateCourseState(state.context)
        }

        else -> {
          mockCourses.put(answer, Course(mutableListOf(), mutableListOf(), "", mockGradeTable))

          send(
            state.context,
          ) {
            +"Курс $answer успешно создан"
          }
          StartState(state.context)
        }
      }
    }

    strictlyOn<PickACourseState> { state ->
      val message = waitTextMessage().first()
      val answer = message.content.text

      when {
        answer == "/stop" -> {
          MenuState(state.context)
        }

        else -> {
          bot.send(
            state.context,
            "Изменить курс $answer:",
            replyMarkup =
            inlineKeyboard {
              row {
                dataButton("Добавить ученика", "add a student")
              }
              row {
                dataButton("Убрать ученика", "remove a student")
              }
              row {
                dataButton("Добавить преподавателя", "add a teacher")
              }
              row {
                dataButton("Убрать преподавателя", "remove a teacher")
              }
              row {
                dataButton("Изменить описание", "edit description")
              }
            },

          )
          mockCourses[answer]?.let { EditCourseState(state.context, it, answer) }
        }
      }
    }

    strictlyOn<EditCourseState> { state ->
      val callback = waitDataCallbackQuery().first()
      val data = callback.data
      answerCallbackQuery(callback)
      when {
        data == "add a student" -> {
          send(
            state.context,
            "Введите ID ученика, которого хотите добавить на курс ${state.courseName}",
            replyMarkup = ReplyKeyboardRemove(),
          )
          AddStudentState(state.context, state.course, state.courseName)
        }

        data == "remove a student" -> {
          send(
            state.context,
          ) {
            +"Введите ID ученика, которого хотите убрать с курса ${state.courseName}"
          }
          RemoveStudentState(state.context, state.course, state.courseName)
        }

        data == "add a teacher" -> {
          send(
            state.context,
          ) {
            +"Введите ID преподавателя, которого хотите добавить на курс ${state.courseName}"
          }
          AddTeacherState(state.context, state.course, state.courseName)
        }

        data == "remove a teacher" -> {
          send(
            state.context,
          ) {
            +"Введите ID преподавателя, которого хотите убрать с курса ${state.courseName}"
          }
          RemoveTeacherState(state.context, state.course, state.courseName)
        }

        data == "edit description" -> {
          send(
            state.context,
          ) {
            +"Введите новое описание курса ${state.course}. Текущее описание:" + newLine + newLine
            +state.course.description
          }
          EditDescriptionState(state.context, state.course, state.courseName)
        }

        else -> EditCourseState(state.context, state.course, state.courseName)
      }
    }

    strictlyOn<AddStudentState> { state ->
      val message = waitTextMessage().first()
      val id = message.content.text
      when {
        id == "/stop" -> StartState(state.context)

        !mockStudents.containsKey(id) -> {
          send(
            state.context,
            "Ученика с идентификатором $id не существует",
            replyMarkup = ReplyKeyboardRemove(),
          )
          AddStudentState(state.context, state.course, state.courseName)
        }

        state.course.students.contains(Student(id)) -> {
          send(
            state.context,
            "Ученик $id уже есть на курсе ${state.courseName}",
            replyMarkup = ReplyKeyboardRemove(),
          )
          StartState(state.context)
        }

        else -> {
          state.course.students.addLast(Student(id))
          send(
            state.context,
            "Ученик $id успешно добавлен на курс ${state.courseName}",
            replyMarkup = ReplyKeyboardRemove(),
          )
          StartState(state.context)
        }
      }
    }

    strictlyOn<RemoveStudentState> { state ->
      val message = waitTextMessage().first()
      val id = message.content.text
      when {
        id == "/stop" -> MenuState(state.context)

        !mockStudents.containsKey(id) -> {
          send(
            state.context,
            "Ученика с идентификатором $id не существует",
            replyMarkup = ReplyKeyboardRemove(),
          )
          RemoveStudentState(state.context, state.course, state.courseName)
        }

        else -> {
          if (state.course.students.remove(Student(id))) {
            send(
              state.context,
              "Ученик $id успешно удалён с курса ${state.courseName}",
              replyMarkup = ReplyKeyboardRemove(),
            )
          } else {
            send(
              state.context,
              "Ученика $id нет на курсе ${state.courseName}",
              replyMarkup = ReplyKeyboardRemove(),
            )
          }
          StartState(state.context)
        }
      }
    }

    strictlyOn<AddTeacherState> { state ->
      val message = waitTextMessage().first()
      val id = message.content.text
      when {
        id == "/stop" -> MenuState(state.context)

        !mockTeachers.containsKey(id) -> {
          send(
            state.context,
            "Преподавателя с идентификатором $id не существует",
            replyMarkup = ReplyKeyboardRemove(),
          )
          AddTeacherState(state.context, state.course, state.courseName)
        }

        state.course.teachers.contains(Teacher(id)) -> {
          send(
            state.context,
            "Преподаватель $id уже есть на курсе ${state.courseName}",
            replyMarkup = ReplyKeyboardRemove(),
          )
          StartState(state.context)
        }

        else -> {
          state.course.teachers.addLast(Teacher(id))
          send(
            state.context,
            "Преподаватель $id успешно добавлен на курс ${state.courseName}",
            replyMarkup = ReplyKeyboardRemove(),
          )
          StartState(state.context)
        }
      }
    }

    strictlyOn<RemoveTeacherState> { state ->
      val message = waitTextMessage().first()
      val id = message.content.text
      when {
        id == "/stop" -> MenuState(state.context)

        !mockTeachers.containsKey(id) -> {
          send(
            state.context,
            "Преподавателя с идентификатором $id не существует",
            replyMarkup = ReplyKeyboardRemove(),
          )
          RemoveTeacherState(state.context, state.course, state.courseName)
        }

        else -> {
          if (state.course.teachers.remove(Teacher(id))) {
            send(
              state.context,
              "Преподаватель $id успешно удалён с курса ${state.courseName}",
              replyMarkup = ReplyKeyboardRemove(),
            )
          } else {
            send(
              state.context,
              "Преподавателя $id нет на курсе ${state.courseName}",
              replyMarkup = ReplyKeyboardRemove(),
            )
          }
          StartState(state.context)
        }
      }
    }

    strictlyOn<EditDescriptionState> { state ->
      val message = waitTextMessage().first()
      val answer = message.content.text
      when {
        answer == "/stop" -> MenuState(state.context)

        else -> {
          state.course.description = answer
          send(
            state.context,
            "Описание курса ${state.courseName} успешно обновлено",
            replyMarkup = ReplyKeyboardRemove(),
          )

          StartState(state.context)
        }
      }
    }

    allUpdatesFlow.subscribeSafelyWithoutExceptions(this) {
      println(it)
    }
  }.second.join()
}
