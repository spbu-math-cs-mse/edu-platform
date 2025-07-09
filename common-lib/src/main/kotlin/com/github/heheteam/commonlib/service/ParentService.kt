package com.github.heheteam.commonlib.service

import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.domain.RichParent
import com.github.heheteam.commonlib.errors.EduPlatformResult
import com.github.heheteam.commonlib.interfaces.ParentId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.StudentStorage
import com.github.heheteam.commonlib.repository.ParentRepository
import com.github.michaelbull.result.binding
import dev.inmo.tgbotapi.types.RawChatId
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

class ParentService(
  private val parentRepository: ParentRepository,
  private val studentStorage: StudentStorage,
  private val database: Database,
) {
  fun getChildren(parentId: ParentId): EduPlatformResult<List<Student>> = binding {
    transaction(database) {
      val parent = parentRepository.findById(parentId).bind()
      parent.children.mapNotNull { studentStorage.resolveStudent(it).bind() }
    }
  }

  fun tryLoginByTgId(id: RawChatId): EduPlatformResult<RichParent?> = binding {
    transaction {
      val parent = parentRepository.findByTgId(id).bind()
      parent
    }
  }

  fun createParent(
    firstName: String,
    lastName: String,
    tgId: RawChatId,
  ): EduPlatformResult<RichParent> = binding {
    transaction {
      val parent = RichParent(ParentId(0L), firstName, lastName, tgId, mutableListOf())
      parentRepository.save(parent).bind()
    }
  }

  fun addChild(parentId: ParentId, studentId: StudentId): EduPlatformResult<Unit> = binding {
    transaction {
      val parent = parentRepository.findById(parentId).bind()
      val student = studentStorage.resolveStudent(studentId).bind()
      if (student != null) {
        parent.addChild(student)
      }
      parentRepository.save(parent)
    }
  }
}
