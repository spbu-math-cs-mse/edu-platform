package com.github.heheteam.commonlib.api

import com.github.heheteam.commonlib.Parent

interface ParentStorage {
  fun createParent(): ParentId

  fun resolveParent(parentId: ParentId): Parent?
}
