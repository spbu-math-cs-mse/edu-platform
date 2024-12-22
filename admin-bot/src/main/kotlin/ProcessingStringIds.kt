package com.github.heheteam.adminbot

import com.github.heheteam.adminbot.Dialogues.duplicatedId
import com.github.heheteam.adminbot.Dialogues.idIsNotLong
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result

fun processStringIds(stringIds: List<String>): Result<List<Long>, String> {
  val ids = mutableListOf<Long>()
  val setOfStringIds = mutableSetOf<String>()

  stringIds.forEach { stringId ->
    if (stringId in setOfStringIds) {
      return Err(duplicatedId(stringId))
    }
    ids.add(stringId.toLongOrNull() ?: return Err(idIsNotLong(stringId)))
    setOfStringIds.add(stringId)
  }

  return Ok(ids)
}
