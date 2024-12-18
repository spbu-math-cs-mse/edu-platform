package com.github.heheteam.commonlib.api

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.Teacher
import com.github.michaelbull.result.Result

interface CoursesDistributor {
    fun addStudentToCourse(
        studentId: StudentId,
        courseId: CourseId,
    ): Result<Unit, BindError<StudentId, CourseId>>

    fun addTeacherToCourse(
        teacherId: TeacherId,
        courseId: CourseId,
    ): Result<Unit, BindError<TeacherId, CourseId>>

    fun removeStudentFromCourse(
        studentId: StudentId,
        courseId: CourseId,
    ): Result<Unit, DeleteError<StudentId>>

    fun removeTeacherFromCourse(
        teacherId: TeacherId,
        courseId: CourseId,
    ): Result<Unit, DeleteError<TeacherId>>

    fun getCourses(): List<Course>

    fun getStudentCourses(studentId: StudentId): List<Course>

    fun getTeacherCourses(teacherId: TeacherId): List<Course>

    fun resolveCourse(courseId: CourseId): Result<Course, ResolveError<CourseId>>

    fun createCourse(description: String): CourseId

    fun getStudents(courseId: CourseId): List<Student>

    fun getTeachers(courseId: CourseId): List<Teacher>
}
