package com.github.heheteam.studentbot.state.quiz

import com.github.heheteam.commonlib.AttachmentKind
import com.github.heheteam.commonlib.LocalMediaAttachment
import com.github.heheteam.commonlib.TextWithMediaAttachments
import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.state.QuestionWithTextAnswerBotState
import com.github.heheteam.studentbot.state.MenuState
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.utils.buildEntities
import dev.inmo.tgbotapi.utils.linkln

class FirstQuestion(context: User, userId: StudentId) :
  QuestionWithTextAnswerBotState<StudentApi, StudentId>(context, userId) {

  override val question: TextWithMediaAttachments by lazy {
    TextWithMediaAttachments(
      text =
        buildEntities {
          +"нужно пройти лабиринт кратчайшим путем, собрать буквы на этом пути, " +
            "а потом составить слово - это название животного?"
        },
      attachments = listOf(LocalMediaAttachment(AttachmentKind.PHOTO, "/maze.jpg")),
    )
  }

  override val correctAnswer: String = "Вомбат"

  override fun checkAnswer(answer: String): Boolean =
    answer.lowercase().trim() == correctAnswer.lowercase()

  override val correctAnswerReply: TextWithMediaAttachments by lazy {
    TextWithMediaAttachments(
      text = buildEntities { +"классб ом-ном-ном" },
      attachments = listOf(LocalMediaAttachment(AttachmentKind.DOCUMENT, "/maze_correct.mp4")),
    )
  }

  override val incorrectAnswerReply: TextWithMediaAttachments by lazy {
    TextWithMediaAttachments(
      text =
        buildEntities(System.lineSeparator()) {
          +"пупупу, ну ты и тупой. Приходи к нам учиться"
          linkln("https://dabromat.ru/start")
        },
      attachments = listOf(LocalMediaAttachment(AttachmentKind.PHOTO, "/maze_incorrect.jpg")),
    )
  }

  override val nextState = { user: User, studentId: StudentId -> MenuState(user, studentId) }

  override val menuState = { user: User, studentId: StudentId -> MenuState(user, studentId) }
}
