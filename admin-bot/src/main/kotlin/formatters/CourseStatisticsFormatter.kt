package com.github.heheteam.adminbot.formatters

import com.github.heheteam.adminbot.CourseStatistics
import dev.inmo.tgbotapi.types.message.textsources.RegularTextSource
import dev.inmo.tgbotapi.types.message.textsources.TextSource
import dev.inmo.tgbotapi.types.message.textsources.bold
import dev.inmo.tgbotapi.utils.RiskFeature

object CourseStatisticsFormatter {
  @OptIn(RiskFeature::class)
  fun format(courseName: String, stats: CourseStatistics): List<TextSource> = buildList {
    add(RegularTextSource("📊 Статистика курса "))
    add(bold(courseName))
    add(RegularTextSource("\n\n"))

    add(RegularTextSource("👥 Участники:\n"))
    add(RegularTextSource("• Студентов: ${stats.studentsCount}\n"))
    add(RegularTextSource("• Преподавателей: ${stats.teachersCount}\n\n"))

    add(RegularTextSource("📚 Учебные материалы:\n"))
    add(RegularTextSource("• Количество серий заданий: ${stats.assignmentsCount}\n"))
    add(RegularTextSource("• Всего задач: ${stats.totalProblems}\n"))
    add(RegularTextSource("• Суммарный максимальный балл: ${stats.totalMaxScore}\n\n"))

    add(RegularTextSource("📝 Статистика решений:\n"))
    add(RegularTextSource("• Всего посылок: ${stats.totalSolutions}\n"))
    add(RegularTextSource("• Проверено задач: ${stats.checkedSolutions}\n"))
    add(RegularTextSource("• Ожидают проверки: ${stats.uncheckedSolutions}\n\n"))

    if (stats.assignments.isNotEmpty()) {
      add(RegularTextSource("📝 Серии заданий:\n"))
      stats.assignments.forEach { assignment ->
        add(RegularTextSource("• ${assignment.description}\n"))
      }
    }

    add(RegularTextSource("\n👨‍🏫 ID преподавателей:\n"))
    if (stats.teachers.isEmpty()) {
      add(RegularTextSource("Нет преподавателей\n"))
    } else {
      stats.teachers.forEach { teacher ->
        add(RegularTextSource("• ${teacher.surname} ${teacher.name} (${teacher.id})\n"))
      }
    }
    add(RegularTextSource("\n"))

    add(RegularTextSource("👨‍🎓 ID студентов:\n"))
    if (stats.students.isEmpty()) {
      add(RegularTextSource("Нет студентов\n"))
    } else {
      stats.students.forEach { student ->
        add(RegularTextSource("• ${student.surname} ${student.name} (${student.id})\n"))
      }
    }
  }
}
