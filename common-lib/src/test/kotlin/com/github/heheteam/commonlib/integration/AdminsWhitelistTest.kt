package com.github.heheteam.commonlib.integration

import com.github.heheteam.commonlib.errors.AdminIsNotWhitelistedError
import com.github.heheteam.commonlib.util.buildData
import com.github.michaelbull.result.get
import dev.inmo.tgbotapi.types.toChatId
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

class AdminsWhitelistTest : IntegrationTestEnvironment() {
  @Test
  fun `random person can't create an account`() = runTest {
    val apis = createDefaultApis()
    buildData(apis) {
      val randomTgId = 10000L.toChatId()
      assertFalse(apis.adminApi.tgIdIsInWhitelist(randomTgId).value)
      val result = apis.adminApi.createAdmin("Name", "Surname", randomTgId.chatId.long)
      assertTrue(result.isErr)
      assertTrue(result.error.error is AdminIsNotWhitelistedError)
    }
  }

  @Test
  fun `admin account creation`() = runTest {
    val api = createDefaultApis()
    buildData(api) {
      val randomTgId = 10000L.toChatId()
      assertFalse(apis.adminApi.tgIdIsInWhitelist(randomTgId).value)
      apis.adminApi.addTgIdToWhitelist(randomTgId)
      val result = apis.adminApi.createAdmin("Name", "Surname", randomTgId.chatId.long)
      assertTrue(result.isOk)
    }
  }
}
