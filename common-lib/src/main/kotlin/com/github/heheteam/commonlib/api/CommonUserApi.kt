package com.github.heheteam.commonlib.api

import com.github.heheteam.commonlib.errors.NumberedError
import com.github.michaelbull.result.Result

interface CommonUserApi<UserId> {
  fun resolveCurrentQuestState(userId: UserId): Result<String?, NumberedError>

  fun saveCurrentQuestState(userId: UserId, questState: String): Result<Unit, NumberedError>
}
