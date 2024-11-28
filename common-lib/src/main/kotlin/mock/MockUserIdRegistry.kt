package com.github.heheteam.commonlib.mock

import com.github.heheteam.commonlib.api.*
import dev.inmo.tgbotapi.types.UserId

class MockAdminIdRegistry(
  val usedId: Long,
) : AdminIdRegistry {
  override fun getUserId(tgId: UserId): AdminId = AdminId(usedId)
}

class MockStudentIdRegistry(
  val usedId: Long,
) : StudentIdRegistry {
  override fun getUserId(tgId: UserId): StudentId = StudentId(usedId)
}

class MockTeacherIdRegistry(
  val usedId: Long,
) : TeacherIdRegistry {
  override fun getUserId(tgId: UserId): TeacherId = TeacherId(usedId)
}

class MockParentIdRegistry(
  val usedId: Long,
) : ParentIdRegistry {
  override fun getUserId(tgId: UserId): ParentId = ParentId(usedId)
}
