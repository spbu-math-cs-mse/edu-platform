package com.github.heheteam.commonlib.statistics

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import java.time.LocalDateTime
import kotlin.test.Test

class MockTeacherStatisticsTest {
    private lateinit var statistics: MockTeacherStatistics
    private val now = LocalDateTime.now()
    private val teacher1Id = "teacher1"

    @BeforeEach
    fun setUp() {
        statistics = MockTeacherStatistics()
    }

    @Test
    fun `test initial state`() {
        val stats = statistics.getTeacherStats(teacher1Id)
        assertEquals(0, stats.totalAssessments)
        assertEquals(0, stats.uncheckedSolutions)
        assertNull(stats.averageCheckTimeHours)
        assertNull(stats.averageResponseTime)
    }

    @Test
    fun `test recording solutions and assessments`() {
        statistics.recordNewSolution(now.minusHours(2))
        statistics.recordNewSolution(now.minusHours(1))

        assertEquals(2, statistics.getTeacherStats(teacher1Id).uncheckedSolutions)
        
        statistics.recordAssessment(teacher1Id, now)
        
        val stats = statistics.getTeacherStats(teacher1Id)
        assertEquals(1, stats.totalAssessments)
        assertEquals(1, stats.uncheckedSolutions)
    }

    @Test
    fun `test average check time calculation`() {
        statistics.recordAssessment(teacher1Id, now.minusHours(2))
        statistics.recordAssessment(teacher1Id, now.minusHours(1))
        statistics.recordAssessment(teacher1Id, now)
        
        val stats = statistics.getTeacherStats(teacher1Id)
        assertEquals(1.5, stats.averageCheckTimeHours)
    }

    @Test
    fun `test global statistics`() {
        val now = LocalDateTime.now()
        
        statistics.recordNewSolution(now)
        statistics.recordNewSolution(now.plusHours(1))
        
        statistics.recordAssessment("teacher1", now.plusHours(2))
        statistics.recordAssessment("teacher2", now.plusHours(3))
        
        val globalStats = statistics.getGlobalStats()
        assertEquals(0, globalStats.totalUncheckedSolutions)
        assertTrue(globalStats.averageCheckTimeHours > 0)
    }
} 