package com.github.heheteam.commonlib.domain

import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.interfaces.ParentId
import com.github.heheteam.commonlib.interfaces.StudentId
import dev.inmo.tgbotapi.types.RawChatId

@Suppress("LongParameterList") // it is only expected that it is big
class RichParent(
  val id: ParentId,
  var firstName: String,
  var lastName: String,
  val tgId: RawChatId,
  var lastQuestState: String? = null,
  val from: String?,
  val children: MutableList<StudentId>,
) {
  fun addChild(student: Student) {
    children.add(student.id)
  }
}
