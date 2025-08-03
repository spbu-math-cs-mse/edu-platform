package com.github.heheteam.commonlib.database.table

import org.jetbrains.exposed.sql.Table

object StudentAnswersTable : Table("student_answers") {
  val quizId = long("quiz_id").references(QuizTable.id)
  val studentId = long("student_id")
  val chosenAnswerIndex = integer("chosen_answer_index")

  override val primaryKey = PrimaryKey(quizId, studentId)
}
