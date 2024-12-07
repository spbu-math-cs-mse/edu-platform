package com.github.heheteam.commonlib.mock

import com.github.heheteam.commonlib.Parent
import com.github.heheteam.commonlib.api.ParentId
import com.github.heheteam.commonlib.api.ParentStorage

class MockParentStorage : ParentStorage {
  override fun createParent(): ParentId = ParentId(0)

  override fun resolveParent(parentId: ParentId): Parent? = Parent(parentId, children = listOf())
}
