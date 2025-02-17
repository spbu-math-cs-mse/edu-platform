package com.github.heheteam.teacherbot

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.Grade
import com.github.heheteam.commonlib.api.CoursesDistributor
import com.github.heheteam.commonlib.api.GradeTable
import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.api.TeacherId

class CoursesStatisticsResolver(
  private val coursesDistributor: CoursesDistributor,
  private val gradeTable: GradeTable,
) {
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
