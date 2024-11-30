package com.github.heheteam.adminbot

import com.github.heheteam.commonlib.*
import com.github.heheteam.commonlib.api.*
import dev.inmo.tgbotapi.types.Username
import java.time.LocalDateTime

class AdminCore(
  private val gradeTable: GradeTable,
  private val scheduledMessagesDistributor: ScheduledMessagesDistributor,
  private val coursesTable: MutableMap<String, Course>,
  private val studentsTable: MutableMap<StudentId, Student>,
  private val teachersTable: MutableMap<TeacherId, Teacher>,
  private val adminsTable: List<Username>,
  private val coursesDistributor: CoursesDistributor,
) : GradeTable,
  ScheduledMessagesDistributor {
  override fun addAssessment(
    teacherId: TeacherId,
    solutionId: SolutionId,
    assessment: SolutionAssessment,
  ) = gradeTable.addAssessment(teacherId, solutionId, assessment)

  override fun getStudentPerformance(
    studentId: StudentId,
    solutionDistributor: SolutionDistributor,
  ): Map<ProblemId, Grade> = gradeTable.getStudentPerformance(studentId, solutionDistributor)

  override fun addMessage(message: ScheduledMessage) = scheduledMessagesDistributor.addMessage(message)

  override fun getMessagesUpToDate(date: LocalDateTime): List<ScheduledMessage> = scheduledMessagesDistributor.getMessagesUpToDate(date)

  override fun markMessagesUpToDateAsSent(date: LocalDateTime) = scheduledMessagesDistributor.markMessagesUpToDateAsSent(date)

  fun courseExists(courseName: String) = coursesTable.containsKey(courseName)

  fun addCourse(
    courseName: String,
    course: Course,
  ) {
    coursesTable[courseName] = course
  }

  fun getCourse(courseName: String) = coursesTable[courseName]

  fun getCourses() = coursesTable.toMap()

  fun studentExists(id: StudentId) = studentsTable.containsKey(id)

  fun teacherExists(id: TeacherId) = teachersTable.containsKey(id)

  fun studiesIn(
    id: StudentId,
    course: Course,
  ): Boolean = false

  fun registerForCourse(
    studentId: StudentId,
    courseId: CourseId,
  ) {
    coursesDistributor.addToCourse(studentId, courseId)
  }

  fun removeTeacher(
    teacherId: TeacherId,
    courseId: CourseId,
  ): Boolean = false

  fun removeStudent(
    studentId: StudentId,
    courseId: CourseId,
  ): Boolean {
    TODO()
  }

  override fun assessSolution(
    solutionId: SolutionId,
    teacherId: TeacherId,
    assessment: SolutionAssessment,
    gradeTable: GradeTable,
    teacherStatistics: TeacherStatistics,
    timestamp: LocalDateTime
  ) = gradeTable.assessSolution(
    solutionId,
    teacherId, 
    assessment,
    gradeTable,
    teacherStatistics,
    timestamp
  )

  override fun isChecked(solutionId: SolutionId): Boolean = gradeTable.isChecked(solutionId)
}
