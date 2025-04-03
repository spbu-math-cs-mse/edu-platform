package com.github.heheteam.commonlib.api

import org.jetbrains.exposed.sql.Database

data class ApiCollection(
  val studentApi: StudentApi,
  val teacherApi: TeacherApi,
  val adminApi: AdminApi,
  val parentApi: ParentApi,
)

class ApiFabric(private val database: Database) {
  fun createApis(): ApiCollection {

  }
}
