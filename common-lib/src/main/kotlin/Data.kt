@file:Suppress("unused")

package com.github.heheteam.commonlib

import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.UserId

data class Student(
  val id: String,
)

data class Parent(
  val id: String,
  val children: List<Student>,
)

data class Teacher(
  val id: String,
)

data class Problem(
  val id: String,
)

enum class SolutionType {
  TEXT,
  PHOTO,
  PHOTOS,
  DOCUMENT,
}

data class Solution(
  val id: String,
  val studentId: String,
  val chatId: RawChatId,
  val messageId: MessageId,
  val problem: Problem,
  val content: SolutionContent,
  val type: SolutionType,
  val timestamp: java.time.LocalDateTime = java.time.LocalDateTime.now(),
)
typealias Grade = Int

class Course(
  val id: String,
  val teachers: MutableList<Teacher>,
  val students: MutableList<Student>,
  var description: String,
  val gradeTable: GradeTable,
)

data class SolutionContent(
  val fileIds: List<String>? = null,
  val text: String? = null,
)

data class SolutionAssessment(
  val grade: Grade,
  val comments: String,
)

interface GradeTable {
  fun addAssessment(
    student: Student,
    teacher: Teacher,
    solution: Solution,
    assessment: SolutionAssessment,
  )

  fun getGradeMap(): Map<Student, Map<Problem, Grade>>
}

interface SolutionDistributor {
  fun inputSolution(
    studentId: String,
    chatId: RawChatId,
    messageID: MessageId,
    solutionContent: SolutionContent,
  ): Solution

  fun querySolution(teacherId: String): Solution?

  fun assessSolution(
    solution: Solution,
    teacherId: String,
    assessment: SolutionAssessment,
    gradeTable: GradeTable,
    timestamp: java.time.LocalDateTime = java.time.LocalDateTime.now(),
  )
}

interface CoursesDistributor {
  fun addRecord(
    studentId: String,
    courseId: String,
  )

  fun getCourses(studentId: String): String

  fun getAvailableCourses(studentId: String): MutableList<Pair<Course, Boolean>>
}

interface UserIdRegistry {
  fun getUserId(tgId: UserId): String?

  fun setUserId(
    tgId: UserId,
  )

  fun getRegistry(): Map<UserId, String>
}
