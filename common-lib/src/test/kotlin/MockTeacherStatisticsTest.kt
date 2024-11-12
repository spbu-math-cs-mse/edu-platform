import com.github.heheteam.commonlib.statistics.MockTeacherStatistics
import com.github.heheteam.commonlib.statistics.TeacherStatsData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class MockTeacherStatisticsTest {

    private lateinit var mockTeacherStatistics: MockTeacherStatistics

    @BeforeEach
    fun setUp() {
        mockTeacherStatistics = MockTeacherStatistics()
    }

    @Test
    fun `test recordAssessment and getTeacherStats`() {
        val teacherId = "teacher1"
        val now = LocalDateTime.now()

        mockTeacherStatistics.recordAssessment(teacherId, now.minusDays(1))
        mockTeacherStatistics.recordAssessment(teacherId, now)

        val stats = mockTeacherStatistics.getTeacherStats(teacherId)

        assertEquals(2, stats.totalAssessments)
        assertEquals(now, stats.lastAssessmentTime)
        assertEquals(1.0, stats.averageAssessmentsPerDay, 0.01)
    }

    @Test
    fun `test getTeacherStats with no assessments`() {
        val teacherId = "teacher2"
        val stats = mockTeacherStatistics.getTeacherStats(teacherId)

        assertEquals(0, stats.totalAssessments)
        assertEquals(null, stats.lastAssessmentTime)
        assertEquals(0.0, stats.averageAssessmentsPerDay)
    }

    @Test
    fun `test getAllTeachersStats`() {
        val teacherId1 = "teacher1"
        val teacherId2 = "teacher2"
        val now = LocalDateTime.now()

        mockTeacherStatistics.recordAssessment(teacherId1, now)
        mockTeacherStatistics.recordAssessment(teacherId2, now.minusDays(1))

        val allStats = mockTeacherStatistics.getAllTeachersStats()

        assertEquals(2, allStats.size)
        assertEquals(1, allStats[teacherId1]?.totalAssessments)
        assertEquals(1, allStats[teacherId2]?.totalAssessments)
    }
} 