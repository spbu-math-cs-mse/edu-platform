package com.github.heheteam.commonlib.googlesheets

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.Grade
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.api.ProblemId
import com.github.heheteam.commonlib.api.SpreadsheetId
import com.github.heheteam.commonlib.api.StudentId
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.Permission
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
import com.google.api.services.sheets.v4.model.Spreadsheet
import com.google.api.services.sheets.v4.model.SpreadsheetProperties
import com.google.api.services.sheets.v4.model.UnmergeCellsRequest
import com.google.api.services.sheets.v4.model.UpdateCellsRequest
import com.google.api.services.sheets.v4.model.UpdateDimensionPropertiesRequest
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials

private const val RATING_SHEET_TITLE: String = "Рейтинг"

class GoogleSheetsService(serviceAccountKeyFile: String) {
  private val apiClient: Sheets
  private val driveClient: Drive
  private val tableComposer: TableComposer = TableComposer()

  init {
    val credentials =
      GoogleCredentials.fromStream(
          object {}.javaClass.classLoader.getResourceAsStream(serviceAccountKeyFile)
        )
        .createScoped(listOf("https://www.googleapis.com/auth/spreadsheets", DriveScopes.DRIVE))

    apiClient =
      Sheets.Builder(
          com.google.api.client.http.javanet.NetHttpTransport(),
          com.google.api.client.json.gson.GsonFactory.getDefaultInstance(),
          HttpCredentialsAdapter(credentials),
        )
        .setApplicationName("GoogleSheetsService")
        .build()

    driveClient =
      Drive.Builder(
          com.google.api.client.http.javanet.NetHttpTransport(),
          com.google.api.client.json.gson.GsonFactory.getDefaultInstance(),
          HttpCredentialsAdapter(credentials),
        )
        .setApplicationName("GoogleDriveService")
        .build()
  }

  fun createCourseSpreadsheet(course: Course): SpreadsheetId {
    val spreadsheetProperties = SpreadsheetProperties().setTitle(course.name)
    val spreadsheet = Spreadsheet().setProperties(spreadsheetProperties)
    val createdSpreadsheet = apiClient.spreadsheets().create(spreadsheet).execute()

    val permission =
      Permission().apply {
        this.type = "anyone"
        this.role = "reader"
      }

    driveClient.permissions().create(createdSpreadsheet.spreadsheetId, permission).execute()

    val addSheetRequest =
      AddSheetRequest().setProperties(SheetProperties().setTitle(RATING_SHEET_TITLE))
    val batchUpdateRequest =
      BatchUpdateSpreadsheetRequest().setRequests(listOf(Request().setAddSheet(addSheetRequest)))
    apiClient
      .spreadsheets()
      .batchUpdate(createdSpreadsheet.spreadsheetId, batchUpdateRequest)
      .execute()
    return SpreadsheetId(createdSpreadsheet.spreadsheetId)
  }

  fun updateRating(
    courseSpreadsheetId: String,
    course: Course,
    assignments: List<Assignment>,
    problems: List<Problem>,
    students: List<Student>,
    performance: Map<StudentId, Map<ProblemId, Grade?>>,
  ) {
    val spreadsheet = apiClient.spreadsheets().get(courseSpreadsheetId).execute()
    val sheetId =
      spreadsheet.sheets.first { it.properties.title == RATING_SHEET_TITLE }.properties.sheetId
    val table: ComposedTable =
      tableComposer.composeTable(course, problems, assignments, students, performance)

    val batchUpdateRequest =
      BatchUpdateSpreadsheetRequest()
        .setRequests(
          generateUnmergeRequests(sheetId) +
            generateUpdateRequests(table.cells, sheetId) +
            generateResizeRequests(table.columnWidths, sheetId) +
            generateMergeRequests(table.cells, sheetId)
        )
    clearSheet(spreadsheet.spreadsheetId, RATING_SHEET_TITLE)
    apiClient.spreadsheets().batchUpdate(courseSpreadsheetId, batchUpdateRequest).execute()
  }

  private fun clearSheet(spreadsheetId: String, sheetName: String) {
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
}
