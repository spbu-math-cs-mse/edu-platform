package com.github.heheteam.commonlib.googlesheets

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.Grade
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.api.AssignmentId
import com.github.heheteam.commonlib.api.ProblemId
import com.github.heheteam.commonlib.api.StudentId
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.AddSheetRequest
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest
import com.google.api.services.sheets.v4.model.ClearValuesRequest
import com.google.api.services.sheets.v4.model.DimensionProperties
import com.google.api.services.sheets.v4.model.DimensionRange
import com.google.api.services.sheets.v4.model.GridRange
import com.google.api.services.sheets.v4.model.MergeCellsRequest
import com.google.api.services.sheets.v4.model.Request
import com.google.api.services.sheets.v4.model.RowData
import com.google.api.services.sheets.v4.model.SheetProperties
import com.google.api.services.sheets.v4.model.UnmergeCellsRequest
import com.google.api.services.sheets.v4.model.UpdateCellsRequest
import com.google.api.services.sheets.v4.model.UpdateDimensionPropertiesRequest
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials

class GoogleSheetsService(serviceAccountKeyFile: String, private val spreadsheetId: String) {
  private val apiClient: Sheets

  init {
    val credentials =
      GoogleCredentials.fromStream(
          object {}.javaClass.classLoader.getResourceAsStream(serviceAccountKeyFile)
        )
        .createScoped(listOf("https://www.googleapis.com/auth/spreadsheets"))

    apiClient =
      Sheets.Builder(
          com.google.api.client.http.javanet.NetHttpTransport(),
          com.google.api.client.json.gson.GsonFactory.getDefaultInstance(),
          HttpCredentialsAdapter(credentials),
        )
        .setApplicationName("GoogleSheetsService")
        .build()
  }

  private fun createCourseSheet(course: Course) {
    val addSheetRequest = AddSheetRequest().setProperties(SheetProperties().setTitle(course.name))

    val batchUpdateRequest =
      BatchUpdateSpreadsheetRequest().setRequests(listOf(Request().setAddSheet(addSheetRequest)))

    apiClient.spreadsheets().batchUpdate(spreadsheetId, batchUpdateRequest).execute()
  }

  fun updateRating(
    course: Course,
    assignments: List<Assignment>,
    problems: List<Problem>,
    students: List<Student>,
    performance: Map<StudentId, Map<ProblemId, Grade?>>,
  ) {
    var spreadsheet = apiClient.spreadsheets().get(spreadsheetId).execute()
    val sheetNames = spreadsheet.sheets.map { it.properties.title }
    if (course.name !in sheetNames) {
      createCourseSheet(course)
      spreadsheet = apiClient.spreadsheets().get(spreadsheetId).execute()
    }
    val sheetId = spreadsheet.sheets.first { it.properties.title == course.name }.properties.sheetId
    val table: ComposedTable = composeTable(course, problems, assignments, students, performance)

    val batchUpdateRequest =
      BatchUpdateSpreadsheetRequest()
        .setRequests(
          generateUnmergeRequests(sheetId) +
            generateUpdateRequests(table.cells, sheetId) +
            generateResizeRequests(table.columnWidths, sheetId) +
            generateMergeRequests(table.cells, sheetId)
        )
    clearSheet(course.name)
    apiClient.spreadsheets().batchUpdate(spreadsheetId, batchUpdateRequest).execute()
  }

  private fun clearSheet(sheetName: String) {
    apiClient
      .spreadsheets()
      .values()
      .clear(spreadsheetId, sheetName, ClearValuesRequest())
      .execute()
  }

  private fun generateUnmergeRequests(sheetId: Int?): List<Request?> =
    listOf(
      Request().setUnmergeCells(UnmergeCellsRequest().setRange(GridRange().setSheetId(sheetId)))
    )

  private fun generateMergeRequests(data: List<List<FormattedCell>>, sheetId: Int?): List<Request> =
    data
      .mapIndexed { row, rowList ->
        var column = 0
        rowList.mapNotNull { cell ->
          var request: Request? = null
          if (cell.width > 1) {
            request =
              Request()
                .setMergeCells(
                  MergeCellsRequest()
                    .setMergeType("MERGE_ALL")
                    .setRange(gridRange(sheetId, row, row, column, column + cell.width))
                )
          }
          column += cell.width
          return@mapNotNull request
        }
      }
      .flatten()
      .toList()

  private fun generateResizeRequests(columWidths: List<Int?>, sheetId: Int?): List<Request> =
    columWidths
      .mapIndexedNotNull { column, width ->
        width ?: return@mapIndexedNotNull null

        Request()
          .setUpdateDimensionProperties(
            UpdateDimensionPropertiesRequest()
              .setRange(
                DimensionRange()
                  .setSheetId(sheetId)
                  .setDimension("COLUMNS")
                  .setStartIndex(column)
                  .setEndIndex(column + 1)
              )
              .setProperties(DimensionProperties().setPixelSize(width))
              .setFields("pixelSize")
          )
      }
      .toList()

  private fun generateUpdateRequests(
    data: List<List<FormattedCell>>,
    sheetId: Int?,
  ): List<Request> =
    data.flatMapIndexed { rowIndex, row ->
      val rowExtended = row.flatMap { cell -> List(cell.width) { cell } }

      listOf(
        Request()
          .setUpdateCells(
            UpdateCellsRequest()
              .setRange(
                gridRange(
                  sheetId,
                  rowIndex,
                  rowIndex + 1,
                  0,
                  row.fold(0) { acc, cell -> acc + cell.width },
                )
              )
              .setRows(listOf(RowData().setValues(rowExtended.map { it.toCellData() })))
              .setFields(
                "userEnteredValue," +
                  "userEnteredFormat.textFormat.bold," +
                  "userEnteredFormat.horizontalAlignment," +
                  "userEnteredFormat.Borders"
              )
          )
      )
    }

  private fun gridRange(
    sheetId: Int?,
    startRow: Int,
    endRow: Int,
    startColumn: Int,
    endColumn: Int,
  ): GridRange =
    GridRange()
      .setSheetId(sheetId)
      .setStartRowIndex(startRow)
      .setEndRowIndex(endRow + 1)
      .setStartColumnIndex(startColumn)
      .setEndColumnIndex(endColumn)

  private fun composeTable(
    course: Course,
    problems: List<Problem>,
    assignments: List<Assignment>,
    students: List<Student>,
    performance: Map<StudentId, Map<ProblemId, Grade?>>,
  ): ComposedTable {
    val sortedProblems =
      problems.sortedWith(compareBy<Problem> { it.assignmentId.id }.thenBy { it.serialNumber })
    val assignmentSizes = mutableMapOf<AssignmentId, Int>()

    for (problem in sortedProblems) {
      assignmentSizes[problem.assignmentId] =
        assignmentSizes[problem.assignmentId]?.let { i -> i + 1 } ?: 1
    }

    return ComposedTable(
      composeHeader(course, assignments, assignmentSizes, sortedProblems) +
        composeGrades(students, sortedProblems, performance),
      listOf(30, null, null) + List<Int?>(sortedProblems.size) { 40 },
    )
  }

  private fun composeHeader(
    course: Course,
    sortedAssignments: List<Assignment>,
    assignmentSizes: MutableMap<AssignmentId, Int>,
    sortedProblems: List<Problem>,
  ) =
    listOf(
      // Row 1
      // Name of the course
      listOf(FormattedCell(course.name, DataType.STRING, 3).centerAlign().bold().borders(2)) +
        // Assignments
        sortedAssignments.map {
          FormattedCell(it.description, DataType.STRING, assignmentSizes[it.id] ?: 0)
            .bold()
            .borders(2)
            .centerAlign()
        },
      // Row 2
      listOf("id", "surname", "name").map { FormattedCell(it, DataType.STRING).bold().borders() } +
        sortedProblems.map {
          FormattedCell(it.number, DataType.STRING).bold().borders().centerAlign()
        },
    )

  private fun composeGrades(
    students: List<Student>,
    sortedProblems: List<Problem>,
    performance: Map<StudentId, Map<ProblemId, Grade?>>,
  ) =
    students.map { student ->
      listOf(student.id.id, student.surname, student.name).map {
        FormattedCell(it.toString(), DataType.STRING).borders()
      } +
        sortedProblems
          .asSequence()
          .map { problem ->
            val grades = performance[student.id]
            if (grades != null) gradeToString(problem.id, grades) else ""
          }
          .map { FormattedCell(it, DataType.STRING).topBorder().bottomBorder().centerAlign() }
          .mapIndexed { index, cell ->
            if (
              index == sortedProblems.size - 1 ||
                sortedProblems[index].assignmentId != sortedProblems[index + 1].assignmentId
            ) {
              cell.rightBorder()
            }
            cell
          }
    }

  private fun gradeToString(problemId: ProblemId, grades: Map<ProblemId, Grade?>): String =
    if (grades.containsKey(problemId)) {
      val grade = grades[problemId]
      when {
        grade == null -> "П"
        else -> grade.toString()
      }
    } else {
      ""
    }
}
