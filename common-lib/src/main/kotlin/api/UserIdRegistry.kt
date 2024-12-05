package com.github.heheteam.commonlib.api

import com.github.michaelbull.result.Result
import dev.inmo.tgbotapi.types.UserId

interface AdminIdRegistry {
  fun getUserId(tgId: UserId): Result<AdminId, ResolveError<UserId>>
}

interface StudentIdRegistry {
  fun getUserId(tgId: UserId): Result<StudentId, ResolveError<UserId>>
}

interface TeacherIdRegistry {
  fun getUserId(tgId: UserId): Result<TeacherId, ResolveError<UserId>>
}

interface ParentIdRegistry {
  fun getUserId(tgId: UserId): Result<ParentId, ResolveError<UserId>>
}
