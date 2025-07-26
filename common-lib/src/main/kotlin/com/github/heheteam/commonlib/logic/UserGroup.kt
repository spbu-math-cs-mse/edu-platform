package com.github.heheteam.commonlib.logic

import com.github.heheteam.commonlib.interfaces.CourseId

sealed interface UserGroup {
  data class CourseGroup(val courseId: CourseId) : UserGroup

  class CompletedQuest : UserGroup

  class AllRegisteredUsers : UserGroup
}
