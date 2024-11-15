package com.github.heheteam.adminbot

import Course
import GradeTable
import Student
import Teacher
import dev.inmo.tgbotapi.types.Username

class AdminCore(
    private val gradeTable: GradeTable,
    private val scheduledMessagesDistributor: ScheduledMessagesDistributor,
    val coursesTable: MutableMap<String, Course>,
    val studentsTable: MutableMap<String, Student>,
    val teachersTable: MutableMap<String, Teacher>,
    val adminsTable: List<Username>,
    ) : GradeTable by gradeTable,
    ScheduledMessagesDistributor by scheduledMessagesDistributor