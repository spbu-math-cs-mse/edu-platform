package com.github.heheteam.commonlib.database.table

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.json.json
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object QuizTable : LongIdTable("quizzes") {
  val courseId = long("course_id")
  val teacherId = long("teacher_id")
  val questionText = text("question_text")
  val answers = json<List<String>>("answers", Json)
  val correctAnswerIndex = integer("correct_answer_index")
  val createdAt = datetime("created_at")
  val duration = long("duration")
  val activationTime = timestamp("activation_time").nullable()
  val isActive = bool("is_active")
}
