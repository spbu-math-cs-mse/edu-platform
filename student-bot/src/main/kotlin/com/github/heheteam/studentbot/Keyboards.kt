package com.github.heheteam.studentbot

import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.row

object Keyboards {
  const val SIGN_UP = "signUpForCourses"
  const val VIEW = "viewMyCourses"
  const val SEND_SOLUTION = "sendSolution"
  const val RETURN_BACK = "back"
  const val APPLY = "apply"
  const val COURSE_ID = "courseId"
  const val PROBLEM_ID = "problemId"
  const val CHECK_GRADES = "checkGrades"
  const val STUDENT_GRADES = "viewStudentGrades"
  const val TOP_GRADES = "viewTopGrades"
  const val FICTITIOUS = "fictitious"
  const val CHECK_DEADLINES = "deadlines"

  fun menu() =
    InlineKeyboardMarkup(
      keyboard =
        matrix {
          row { dataButton("Отправить решение", SEND_SOLUTION) }
          row { dataButton("Проверить успеваемость", CHECK_GRADES) }
          row { dataButton("Посмотреть дедлайны", CHECK_DEADLINES) }
        }
    )
}
