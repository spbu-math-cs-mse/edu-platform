package com.github.heheteam.teacherbot

import GradeTable
import Problem
import Solution
import SolutionContent
import dev.inmo.tgbotapi.types.ChatId

class TeacherCore(
  private val gradeTable: GradeTable,
) : GradeTable by gradeTable {
  fun getUserId(id: ChatId) = id.toString()

  fun querySolution(userId: String) = Solution("", Problem("", "", "", 1, ""), SolutionContent(), SolutionType.TEXT)
}

var mockTgUsername: String = ""
