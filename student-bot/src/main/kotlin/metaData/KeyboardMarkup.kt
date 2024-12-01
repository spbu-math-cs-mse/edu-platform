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
        row { dataButton(description, "${ButtonKey.COURSE_ID} ${course.id.id}") }
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
        row { dataButton(course.name, "${ButtonKey.COURSE_ID} ${course.id.id}") }
      }
      row { dataButton("Назад", ButtonKey.BACK) }
    },
  )

fun buildAssignmentSendingSelector(availableAssignments: List<Assignment>) =
  InlineKeyboardMarkup(
    keyboard =
    matrix {
      availableAssignments.forEach { assignment ->
        row { dataButton(assignment.description, "${ButtonKey.ASSIGNMENT_ID} ${assignment.id.id}") }
      }
      row { dataButton("Назад", ButtonKey.BACK) }
    },
  )

fun buildProblemSendingSelector(availableProblems: List<Problem>) =
  InlineKeyboardMarkup(
    keyboard =
    matrix {
      availableProblems.forEach { problem ->
        row { dataButton(problem.number, "${ButtonKey.PROBLEM_ID} ${problem.id.id}") }
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
