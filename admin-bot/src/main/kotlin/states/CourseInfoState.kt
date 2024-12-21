package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.AdminCore
import com.github.heheteam.adminbot.Keyboards
import com.github.heheteam.adminbot.Keyboards.returnBack
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.util.waitDataCallbackQueryWithUser
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.types.message.textsources.bold
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnCourseInfoState(core: AdminCore) {
  strictlyOn<CourseInfoState> { state ->
    val courses = core.getCourses().values.toList()
    if (courses.isEmpty()) {
      bot.send(state.context, "Не найдено ни одного курса!")
      return@strictlyOn MenuState(state.context)
    }

    val message = bot.send(
      state.context,
      "Выберите курс:",
      replyMarkup = Keyboards.buildCoursesSelector(courses)
    )

    val callback = waitDataCallbackQueryWithUser(state.context.id).first()
    deleteMessage(message)

    if (callback.data == returnBack) {
      return@strictlyOn MenuState(state.context)
    }

    val courseId = callback.data.split(" ").last()
    val course = courses.first { it.id == CourseId(courseId.toLong()) }
    val stats = core.getCourseStatistics(course.id)

    val statsMessage = bot.send(
      state.context,
      replyMarkup = Keyboards.returnBack()
    ) {
      +"📊 Статистика курса "
      +bold(course.name)
      +"\n\n"

      +"👥 Участники:\n"
      +"• Студентов: ${stats.studentsCount}\n"
      +"• Преподавателей: ${stats.teachersCount}\n\n"

      +"📚 Учебные материалы:\n"
      +"• Количество серий заданий: ${stats.assignmentsCount}\n"
      +"• Всего задач: ${stats.totalProblems}\n"
      +"• Суммарный максимальный балл: ${stats.totalMaxScore}\n\n"

      if (stats.assignments.isNotEmpty()) {
        +"📝 Серии заданий:\n"
        stats.assignments.forEach { assignment ->
          +"• ${assignment.description}\n"
        }
        +"\n"
      }

      +"👨‍🏫 ID преподавателей:\n"
      if (stats.teachers.isEmpty()) {
        +"Нет преподавателей\n"
      } else {
        stats.teachers.forEach { teacherId ->
          +"• $teacherId\n"
        }
      }
      +"\n"

      +"👨‍🎓 ID студентов:\n"
      if (stats.students.isEmpty()) {
        +"Нет студентов\n"
      } else {
        stats.students.forEach { studentId ->
          +"• $studentId\n"
        }
      }
    }


    waitDataCallbackQueryWithUser(state.context.id).first()
    deleteMessage(statsMessage)
    MenuState(state.context)
  }
} 