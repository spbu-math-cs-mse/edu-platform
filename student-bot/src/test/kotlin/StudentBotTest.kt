import com.github.heheteam.commonlib.MockCoursesDistributor
import com.github.heheteam.commonlib.MockGradeTable
import com.github.heheteam.commonlib.MockSolutionDistributor
import com.github.heheteam.studentbot.StudentCore
import kotlin.test.Test
import kotlin.test.assertEquals

class StudentBotTest {
  @Test
  fun `testing checking grades`() {
    val mockCoursesDistributor = MockCoursesDistributor()
    val userId = mockCoursesDistributor.singleUserId
    val mockSolutionDistributor = MockSolutionDistributor()
    val studentCore =
      StudentCore(
        mockSolutionDistributor,
        mockCoursesDistributor,
      )
    run {
      val firstCourse = studentCore.getStudentCourses(userId).first()
      val firstAssignment = firstCourse.assignments.first()
      (firstCourse.gradeTable as MockGradeTable).addMockFilling(
        firstAssignment,
        userId,
      )
    }
    // check first input correctness
    val availableCourses = studentCore.getStudentCourses(userId)
    assert(availableCourses.any { it.id == "0" })
    val course = mockCoursesDistributor.getCourses("0").first()
    val assignment = course.assignments.first()
    val grading =
      studentCore.getGradingForAssignment(
        assignment,
        course,
        userId,
      )
    // check output correctness
    assertEquals(listOf(null, 1, null, 0), grading.map { (_, grade) -> grade })
  }
}
