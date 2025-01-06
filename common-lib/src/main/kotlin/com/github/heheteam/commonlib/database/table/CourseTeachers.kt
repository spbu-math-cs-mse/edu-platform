package com.github.heheteam.commonlib.database.table

import org.jetbrains.exposed.dao.id.LongIdTable

object CourseTeachers : LongIdTable("courseTeachers") {
  val teacherId = reference("teacherId", TeacherTable.id)
  val courseId = reference("courseId", CourseTable.id)
}
