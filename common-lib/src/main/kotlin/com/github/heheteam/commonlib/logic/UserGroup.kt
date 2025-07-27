package com.github.heheteam.commonlib.logic

import com.github.heheteam.commonlib.interfaces.CourseId
import kotlinx.serialization.Serializable

@Serializable
sealed interface UserGroup {
  @Serializable
  data class CourseGroup(val courseId: CourseId) : UserGroup {
    override fun toString(): String {
      return "Для курса $courseId"
    }
  }

  @Serializable
  data object OnlyAdmins : UserGroup {
    override fun toString(): String {
      return "Для админов"
    }
  }

  @Serializable
  data object CompletedQuest : UserGroup {
    override fun toString(): String {
      return "Для закончивших квест"
    }
  }

  @Serializable
  data object AllRegisteredUsers : UserGroup {
    override fun toString(): String = "Для всех"
  }
}
