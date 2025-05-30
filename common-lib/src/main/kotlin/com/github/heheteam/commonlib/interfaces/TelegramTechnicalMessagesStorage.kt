package com.github.heheteam.commonlib.interfaces

import com.github.heheteam.commonlib.EduPlatformError
import com.github.heheteam.commonlib.MenuMessageInfo
import com.github.heheteam.commonlib.TelegramMessageInfo
import com.github.michaelbull.result.Result

internal interface TelegramTechnicalMessagesStorage {
  fun registerGroupSubmissionPublication(
    submissionId: SubmissionId,
    telegramMessageInfo: TelegramMessageInfo,
  )

  fun registerPersonalSubmissionPublication(
    submissionId: SubmissionId,
    telegramMessageInfo: TelegramMessageInfo,
  )

  fun resolveGroupMessage(submissionId: SubmissionId): Result<TelegramMessageInfo, EduPlatformError>

  fun resolvePersonalMessage(
    submissionId: SubmissionId
  ): Result<TelegramMessageInfo, EduPlatformError>

  fun updateTeacherMenuMessage(telegramMessageInfo: TelegramMessageInfo)

  fun resolveTeacherMenuMessage(
    teacherId: TeacherId
  ): Result<List<TelegramMessageInfo>, EduPlatformError>

  /**
   * @return TelegramMessageInfo of the menu message if it exists. Otherwise, just returns the chat
   *   id.
   */
  fun resolveTeacherFirstUncheckedSubmissionMessage(
    teacherId: TeacherId
  ): Result<MenuMessageInfo, EduPlatformError>

  fun resolveGroupMenuMessage(
    courseId: CourseId
  ): Result<List<TelegramMessageInfo>, EduPlatformError>

  /**
   * @return TelegramMessageInfo of the menu message if it exists. Otherwise, just returns the chat
   *   id.
   */
  fun resolveGroupFirstUncheckedSubmissionMessage(
    courseId: CourseId
  ): Result<MenuMessageInfo, EduPlatformError>
}
