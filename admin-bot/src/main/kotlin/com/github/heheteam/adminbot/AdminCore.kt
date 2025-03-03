package com.github.heheteam.adminbot

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.api.AssignmentStorage
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.CoursesDistributor
import com.github.heheteam.commonlib.api.ProblemStorage
import com.github.heheteam.commonlib.api.ScheduledMessage
import com.github.heheteam.commonlib.api.ScheduledMessagesDistributor
import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.api.StudentStorage
import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.api.TeacherStorage
import java.time.LocalDateTime

class AdminCore(
  private val scheduledMessagesDistributor: ScheduledMessagesDistributor,
  private val coursesDistributor: CoursesDistributor,
  private val studentStorage: StudentStorage,
  private val teacherStorage: TeacherStorage,
) {
  fun addMessage(message: ScheduledMessage) = scheduledMessagesDistributor.addMessage(message)

  fun getMessagesUpToDate(date: LocalDateTime): List<ScheduledMessage> =
    scheduledMessagesDistributor.getMessagesUpToDate(date)

  fun markMessagesUpToDateAsSent(date: LocalDateTime) =
    scheduledMessagesDistributor.markMessagesUpToDateAsSent(date)

  fun courseExists(courseName: String): Boolean = getCourse(courseName) != null

  fun addCourse(courseName: String) = coursesDistributor.createCourse(courseName)

  fun getCourse(courseName: String): Course? =
    coursesDistributor.getCourses().find { it.name == courseName }

  fun getCourses(): Map<String, Course> =
    coursesDistributor.getCourses().groupBy { it.name }.mapValues { it.value.first() }

  fun studentExists(id: StudentId): Boolean = studentStorage.resolveStudent(id).isOk

  fun teacherExists(id: TeacherId): Boolean = teacherStorage.resolveTeacher(id).isOk

  fun studiesIn(id: StudentId, course: Course): Boolean =
    coursesDistributor.getStudentCourses(id).find { it.id == course.id } != null

  fun teachesIn(id: TeacherId, course: Course): Boolean =
    coursesDistributor.getTeacherCourses(id).find { it.id == course.id } != null

  fun registerStudentForCourse(studentId: StudentId, courseId: CourseId) =
    coursesDistributor.addStudentToCourse(studentId, courseId)

  fun registerTeacherForCourse(teacherId: TeacherId, courseId: CourseId) {
    coursesDistributor.addTeacherToCourse(teacherId, courseId)
  }

  fun removeTeacher(teacherId: TeacherId, courseId: CourseId): Boolean =
    coursesDistributor.removeTeacherFromCourse(teacherId, courseId).isOk

  fun removeStudent(studentId: StudentId, courseId: CourseId): Boolean =
    coursesDistributor.removeStudentFromCourse(studentId, courseId).isOk
}

class AssignmentProblemsResolver(
  private val assignmentStorage: AssignmentStorage,
  private val problemStorage: ProblemStorage,
) {
  fun getAssignments(courseId: CourseId): List<Pair<Assignment, List<Problem>>> {
    val assignments = assignmentStorage.getAssignmentsForCourse(courseId)
    val problems = problemStorage.getProblemsFromCourse(courseId)
    return assignments.map { assignment ->
      Pair(assignment, problems.filter { it.assignmentId == assignment.id })
    }
  }
}
