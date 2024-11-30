package com.github.heheteam.commonlib.api

import dev.inmo.tgbotapi.types.UserId

interface AdminIdRegistry {
  fun getUserId(tgId: UserId): AdminId?
}

interface StudentIdRegistry {
  fun getUserId(tgId: UserId): StudentId?
}

interface TeacherIdRegistry {
  fun getUserId(tgId: UserId): TeacherId?
}

interface ParentIdRegistry {
  fun getUserId(tgId: UserId): ParentId?
}
