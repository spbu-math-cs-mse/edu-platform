package com.github.heheteam.commonlib.statistics

import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.Solution
import com.github.heheteam.commonlib.SolutionContent
import com.github.heheteam.commonlib.SolutionType
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import java.sql.Timestamp
import java.time.LocalDateTime
import kotlin.test.Test

class MockTeacherStatisticsTest {
    private lateinit var statistics: MockTeacherStatistics
    private val now = LocalDateTime.now()
    private val teacher1Id = "teacher1"

    private fun makeSolution(timestamp: LocalDateTime) = Solution(
        "", "", RawChatId(0), MessageId(0), Problem(""),
        SolutionContent(), SolutionType.TEXT, timestamp
    )

    @BeforeEach
    fun setUp() {
        statistics = MockTeacherStatistics()
    }

    @Test
    fun `test initial state`() {
        val stats = statistics.getTeacherStats(teacher1Id)
        assertNull(stats)
        assertEquals(0, statistics.getGlobalStats().totalUncheckedSolutions)
    }

    @Test
    fun `test recording solutions and assessments`() {
        statistics.recordNewSolution(makeSolution(now.minusHours(2)))
        statistics.recordNewSolution(makeSolution(now.minusHours(1)))

        assertEquals(2, statistics.getGlobalStats().totalUncheckedSolutions)

        statistics.recordAssessment(teacher1Id, makeSolution(now.minusHours(2)), now)

        val stats = statistics.getTeacherStats(teacher1Id)
        assertEquals(1, stats!!.totalAssessments)
        assertEquals(1, statistics.getGlobalStats().totalUncheckedSolutions)
    }

    @Test
    fun `test average check time calculation`() {
        val sol1 = makeSolution(now.minusHours(6))
        val sol2 = makeSolution(now.minusHours(2))
        val sol3 = makeSolution(now)
        statistics.recordNewSolution(sol1)
        statistics.recordNewSolution(sol2)
        statistics.recordNewSolution(sol3)
        statistics.recordAssessment(teacher1Id,  sol1, now.minusHours(4))
        statistics.recordAssessment(teacher1Id, sol2, now.minusHours(1))
        statistics.recordAssessment(teacher1Id, sol3, now)

        val stats = statistics.getTeacherStats(teacher1Id)
        assertEquals(1.0 * 60 * 60, stats!!.averageCheckTimeSeconds, 0.01)
    }

    @Test
    fun `test global statistics`() {
        val now = LocalDateTime.now()
        val sol1 = makeSolution(now.minusHours(3))
        val sol2 = makeSolution(now.minusHours(2))

        statistics.recordNewSolution(sol1)
        statistics.recordNewSolution(sol2)

        statistics.recordAssessment("teacher1", sol1, now.minusHours(2))
        statistics.recordAssessment("teacher2", sol2, now.minusHours(1))

        val globalStats = statistics.getGlobalStats()
        assertEquals(0, globalStats.totalUncheckedSolutions)
        assertTrue(globalStats.averageCheckTimeHours > 0)
    }
} 