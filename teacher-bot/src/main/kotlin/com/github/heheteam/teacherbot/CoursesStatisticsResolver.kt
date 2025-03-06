package com.github.heheteam.teacherbot

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.Grade
import com.github.heheteam.commonlib.api.CoursesDistributor
import com.github.heheteam.commonlib.api.GradeTable
import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.api.TeacherId
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class CoursesStatisticsResolver : KoinComponent {
  private val coursesDistributor: CoursesDistributor by inject()
  private val gradeTable: GradeTable by inject()

  fun getAvailableCourses(teacherId: TeacherId): List<Course> =
    coursesDistributor.getTeacherCourses(teacherId)

  fun getGrading(course: Course): List<Pair<StudentId, Grade>> {
    val students = coursesDistributor.getStudents(course.id)
    val grades =
      students.map { student ->
        student.id to gradeTable.getStudentPerformance(student.id).values.filterNotNull().sum()
      }
    return grades
  }

  fun getMaxGrade(): Grade = 5 // TODO: this needs to be fixed properly
}
