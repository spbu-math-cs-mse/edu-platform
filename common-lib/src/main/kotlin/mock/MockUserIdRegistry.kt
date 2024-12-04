package com.github.heheteam.commonlib.mock

import com.github.heheteam.commonlib.api.*
import dev.inmo.tgbotapi.types.UserId

class MockAdminIdRegistry(
  val usedId: Long,
) : AdminIdRegistry {
  override fun getUserId(tgId: UserId): AdminId? = if (tgId.chatId.long == usedId) AdminId(usedId) else null
}

class MockStudentIdRegistry(
  val usedId: Long,
) : StudentIdRegistry {
  override fun getUserId(tgId: UserId): StudentId? = if (tgId.chatId.long == usedId) StudentId(usedId) else null
}

class MockTeacherIdRegistry(
  val usedId: Long,
) : TeacherIdRegistry {
  override fun getUserId(tgId: UserId): TeacherId? = if (tgId.chatId.long == usedId) TeacherId(usedId) else null
}

class MockParentIdRegistry(
  val usedId: Long,
) : ParentIdRegistry {
  override fun getUserId(tgId: UserId): ParentId? = if (tgId.chatId.long == usedId) ParentId(usedId) else null
}
