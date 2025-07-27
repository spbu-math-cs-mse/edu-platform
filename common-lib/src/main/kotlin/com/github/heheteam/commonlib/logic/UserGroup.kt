package com.github.heheteam.commonlib.logic

import com.github.heheteam.commonlib.interfaces.CourseId
import kotlinx.serialization.Serializable

@Serializable
sealed interface UserGroup {
  @Serializable data class CourseGroup(val courseId: CourseId) : UserGroup

  @Serializable class CompletedQuest : UserGroup

  @Serializable class AllRegisteredUsers : UserGroup
}
