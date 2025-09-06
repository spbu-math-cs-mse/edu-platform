package com.github.heheteam.adminbot.states

import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.state.SimpleState

abstract class SimpleAdminState : SimpleState<AdminApi, AdminId>()
