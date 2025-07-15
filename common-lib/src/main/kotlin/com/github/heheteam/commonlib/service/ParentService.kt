package com.github.heheteam.commonlib.service

import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.domain.RichParent
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.heheteam.commonlib.errors.EduPlatformResult
import com.github.heheteam.commonlib.errors.NamedError
import com.github.heheteam.commonlib.interfaces.ParentId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.StudentStorage
import com.github.heheteam.commonlib.repository.ParentRepository
import com.github.heheteam.commonlib.util.raiseError
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
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
      if (parent == null) {
        raiseError(NamedError("Cannot resolve parent with id: $parentId") as EduPlatformError)
      }
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
    transaction(database) {
      val parent = RichParent(ParentId(0L), firstName, lastName, tgId, null, mutableListOf())
      parentRepository.save(parent).bind()
    }
  }

  fun addChild(parentId: ParentId, studentId: StudentId): EduPlatformResult<Unit> = binding {
    transaction(database) {
      val parent = parentRepository.findById(parentId).bind()
      if (parent == null) {
        raiseError(NamedError("Cannot resolve parent with id: $parentId") as EduPlatformError)
      }
      val student = studentStorage.resolveStudent(studentId).bind()
      if (student != null) {
        parent.addChild(student)
      }
      parentRepository.save(parent)
    }
  }

  fun saveCurrentQuestSave(
    userId: ParentId,
    questState: String,
  ): Result<RichParent, EduPlatformError> = binding {
    transaction(database) {
      val parent = parentRepository.findById(userId).bind()
      if (parent == null) {
        Err(NamedError("Cannot resolve student with id: $userId") as EduPlatformError).bind()
      } else {
        parent.lastQuestState = questState
        parentRepository.save(parent).bind()
      }
    }
  }

  fun resolveCurrentQuestState(userId: ParentId): Result<String?, EduPlatformError> = binding {
    transaction(database) {
      val parent = parentRepository.findById(userId).bind()
      if (parent == null) {
        raiseError(NamedError("Cannot resolve parent with id: $userId") as EduPlatformError)
      }
      parent.lastQuestState
    }
  }
}
