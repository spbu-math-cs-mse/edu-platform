package com.github.heheteam.commonlib.domain

import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.interfaces.ParentId
import com.github.heheteam.commonlib.interfaces.StudentId
import dev.inmo.tgbotapi.types.RawChatId

class RichParent(
  val id: ParentId,
  var firstName: String,
  var lastName: String,
  val tgId: RawChatId,
  var lastQuestState: String? = null,
  val children: MutableList<StudentId>,
) {
  fun addChild(student: Student) {
    children.add(student.id)
  }
}
