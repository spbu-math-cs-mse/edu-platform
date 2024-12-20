package com.github.heheteam.studentbot.metaData

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.Problem
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.row

fun buildCoursesSelector(availableCourses: List<Pair<Course, Boolean>>) =
  InlineKeyboardMarkup(
    keyboard =
    matrix {
      availableCourses.forEach { (course, status) ->
        val description = if (status) "${course.name} ✅" else course.name
        row { dataButton(description, "${ButtonKey.COURSE_ID} ${course.id}") }
      }
      row { dataButton("Записаться", ButtonKey.APPLY) }
      row { dataButton("Назад", ButtonKey.BACK) }
    },
  )

fun buildCoursesSendingSelector(availableCourses: List<Course>) =
  InlineKeyboardMarkup(
    keyboard =
    matrix {
      availableCourses.forEach { course ->
        row { dataButton(course.name, "${ButtonKey.COURSE_ID} ${course.id}") }
      }
      row { dataButton("Назад", ButtonKey.BACK) }
    },
  )

fun buildProblemSendingSelector(availableProblems: List<Pair<Assignment, Problem>>) =
  InlineKeyboardMarkup(
    keyboard =
    matrix {
      availableProblems.forEach { (assignment, problem) ->
        row { dataButton("${assignment.description}: ${problem.number}", "${ButtonKey.PROBLEM_ID} ${problem.id}") }
      }
      row { dataButton("Назад", ButtonKey.BACK) }
    },
  )

fun back() =
  InlineKeyboardMarkup(
    keyboard =
    matrix {
      row { dataButton("Назад", ButtonKey.BACK) }
    },
  )

fun menuKeyboard() =
  InlineKeyboardMarkup(
    keyboard =
    matrix {
      row { dataButton("Записаться на курсы", ButtonKey.SIGN_UP) }
      row { dataButton("Посмотреть мои курсы", ButtonKey.VIEW) }
      row { dataButton("Отправить решение", ButtonKey.SEND_SOLUTION) }
      row { dataButton("Проверить успеваемость", ButtonKey.CHECK_GRADES) }
    },
  )
