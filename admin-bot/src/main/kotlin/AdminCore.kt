package com.github.heheteam.adminbot

import com.github.heheteam.commonlib.*
import com.github.heheteam.commonlib.api.*
import java.time.LocalDateTime

class AdminCore(
  private val gradeTable: GradeTable,
  private val scheduledMessagesDistributor: ScheduledMessagesDistributor,
  private val coursesDistributor: CoursesDistributor,
) {
  fun addAssessment(
    teacherId: TeacherId,
    solutionId: SolutionId,
    assessment: SolutionAssessment,
  ) = gradeTable.addAssessment(teacherId, solutionId, assessment)

  fun getStudentPerformance(
    studentId: StudentId,
    solutionDistributor: SolutionDistributor,
  ): Map<ProblemId, Grade> = gradeTable.getStudentPerformance(studentId, solutionDistributor)

  fun addMessage(message: ScheduledMessage) = scheduledMessagesDistributor.addMessage(message)

  fun getMessagesUpToDate(date: LocalDateTime): List<ScheduledMessage> = scheduledMessagesDistributor.getMessagesUpToDate(date)

  fun markMessagesUpToDateAsSent(date: LocalDateTime) = scheduledMessagesDistributor.markMessagesUpToDateAsSent(date)

  fun courseExists(courseName: String): Boolean = TODO()

  fun addCourse(
    courseName: String,
    course: Course,
  ) {
    TODO()
  }

  fun getCourse(courseName: String): Course? = TODO()

  fun getCourses(): Map<String, Course> = TODO()

  fun studentExists(id: StudentId): Boolean = TODO()

  fun teacherExists(id: TeacherId): Boolean = TODO()

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
