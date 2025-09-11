package com.github.heheteam.commonlib.googlesheets

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.Grade
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.domain.RichCourse
import com.github.heheteam.commonlib.errors.BatchUpdateError
import com.github.heheteam.commonlib.errors.ClearSheetError
import com.github.heheteam.commonlib.errors.CreateSheetError
import com.github.heheteam.commonlib.errors.CreateSpreadsheetError
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.heheteam.commonlib.errors.GetSpreadsheetError
import com.github.heheteam.commonlib.errors.SetupPermissionsError
import com.github.heheteam.commonlib.errors.SheetNotFoundError
import com.github.heheteam.commonlib.interfaces.ProblemId
import com.github.heheteam.commonlib.interfaces.SpreadsheetId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.quiz.RichQuiz
import com.github.heheteam.commonlib.util.raiseError
import com.github.michaelbull.result.BindingScope
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.Permission
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.AddSheetRequest
import com.google.api.services.sheets.v4.model.AppendDimensionRequest
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest
import com.google.api.services.sheets.v4.model.ClearValuesRequest
import com.google.api.services.sheets.v4.model.DimensionProperties
import com.google.api.services.sheets.v4.model.DimensionRange
import com.google.api.services.sheets.v4.model.GridRange
import com.google.api.services.sheets.v4.model.MergeCellsRequest
import com.google.api.services.sheets.v4.model.Request
import com.google.api.services.sheets.v4.model.RowData
import com.google.api.services.sheets.v4.model.Sheet
import com.google.api.services.sheets.v4.model.SheetProperties
import com.google.api.services.sheets.v4.model.Spreadsheet
import com.google.api.services.sheets.v4.model.SpreadsheetProperties
import com.google.api.services.sheets.v4.model.UnmergeCellsRequest
import com.google.api.services.sheets.v4.model.UpdateCellsRequest
import com.google.api.services.sheets.v4.model.UpdateDimensionPropertiesRequest
import com.google.api.services.sheets.v4.model.UpdateSheetPropertiesRequest
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import java.io.File

private const val RATING_SHEET_TITLE: String = "Рейтинг"
private const val QUIZZES_SHEET_TITLE: String = "Опросы"

@Suppress("TooManyFunctions") // low-level class, ok to have many functions
class GoogleSheetsServiceImpl(serviceAccountKeyFile: String) : GoogleSheetsService {
  private val apiClient: Sheets
  private val driveClient: Drive
  private val tableComposer: TableComposer = TableComposer()

  init {
    val credentials =
      GoogleCredentials.fromStream(File(serviceAccountKeyFile).inputStream())
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

  override fun createCourseSpreadsheet(
    courseName: String
  ): Result<SpreadsheetId, EduPlatformError> = createSpreadsheetWithName(courseName)

  private fun createSpreadsheetWithName(
    courseName: String
  ): Result<SpreadsheetId, EduPlatformError> = binding {
    val spreadsheetProperties = SpreadsheetProperties().setTitle(courseName)
    val spreadsheet = Spreadsheet().setProperties(spreadsheetProperties)
    val createdSpreadsheet =
      runCatching { apiClient.spreadsheets().create(spreadsheet).execute() }
        .mapError { CreateSpreadsheetError(courseName, it) }
        .bind()

    val permission =
      Permission().apply {
        this.type = "anyone"
        this.role = "reader"
      }
    //    val addQuizzesSheet =
    //    createAddNewSheetRequest(QUIZZES_SHEET_TITLE)
    val addQuizzesSheet = listOf<Request?>()

    runCatching {
        driveClient.permissions().create(createdSpreadsheet.spreadsheetId, permission).execute()
      }
      .mapError { SetupPermissionsError(courseName, it) }
      .bind()

    val defaultSheet = createdSpreadsheet.sheets.first().properties

    val batchUpdateRequest =
      BatchUpdateSpreadsheetRequest()
        .setRequests(
          generateRenameSheetRequest() +
            generateExtendTableRequest(defaultSheet.sheetId) +
            addQuizzesSheet
        )

    runCatching {
        apiClient
          .spreadsheets()
          .batchUpdate(createdSpreadsheet.spreadsheetId, batchUpdateRequest)
          .execute()
      }
      .mapError { CreateSheetError(courseName, it) }
      .bind()

    return@binding SpreadsheetId(createdSpreadsheet.spreadsheetId)
  }

  private fun createAddNewSheetRequest(name: String): List<Request?> =
    listOf(
      Request()
        .setAddSheet(AddSheetRequest().apply { this.properties = SheetProperties().setTitle(name) })
    )

  private fun generateRenameSheetRequest(): List<Request?> =
    listOf(
      Request()
        .setUpdateSheetProperties(
          UpdateSheetPropertiesRequest().apply {
            this.properties = SheetProperties().setTitle(RATING_SHEET_TITLE)
            this.fields = "title"
          }
        )
    )

  private fun generateExtendTableRequest(sheetId: Int?): List<Request?> =
    listOf(
      Request()
        .setAppendDimension(
          AppendDimensionRequest().apply {
            this.sheetId = sheetId
            this.dimension = "COLUMNS"
            this.length = 1e2.toInt()
          }
        ),
      Request()
        .setAppendDimension(
          AppendDimensionRequest().apply {
            this.sheetId = sheetId
            this.dimension = "ROWS"
            this.length = 1e2.toInt()
          }
        ),
    )

  override fun updateRating(
    courseSpreadsheetId: String,
    course: Course,
    assignments: List<Assignment>,
    problems: List<Problem>,
    students: List<Student>,
    performance: Map<StudentId, Map<ProblemId, Grade?>>,
  ): Result<Unit, EduPlatformError> = binding {
    val table: ComposedTable =
      tableComposer.composeTable(course, problems, assignments, students, performance)

    writeTableToSheet(courseSpreadsheetId, RATING_SHEET_TITLE, table)
  }

  override fun updateQuizzesSheet(
    courseSpreadsheetId: String,
    course: RichCourse,
    students: List<Student?>,
    bind: List<RichQuiz>,
  ): Result<Unit, EduPlatformError> = binding {
    //    val table: ComposedTable = tableComposer.composeQuizzesTable(bind,
    // students.filterNotNull())
    //
    //    writeTableToSheet(courseSpreadsheetId, QUIZZES_SHEET_TITLE, table)
  }

  private fun BindingScope<EduPlatformError>.writeTableToSheet(
    courseSpreadsheetId: String,
    sheetName: String,
    table: ComposedTable,
  ) {
    val spreadsheet =
      runCatching { apiClient.spreadsheets().get(courseSpreadsheetId).execute() }
        .mapError { GetSpreadsheetError(courseSpreadsheetId, it) }
        .bind()

    val relevantSheetId =
      spreadsheet.sheets
        .firstOrNull<Sheet> { it.properties.title == sheetName }
        ?.properties
        ?.sheetId
        ?: run {
          createQuizzesSheet(courseSpreadsheetId)
          val spreadsheetRerequested =
            runCatching { apiClient.spreadsheets().get(courseSpreadsheetId).execute() }
              .mapError { GetSpreadsheetError(courseSpreadsheetId, it) }
              .bind()

          val foundSheetId =
            spreadsheetRerequested.sheets
              .firstOrNull { it.properties.title == sheetName }
              ?.properties
              ?.sheetId ?: raiseError(SheetNotFoundError(courseSpreadsheetId, RATING_SHEET_TITLE))
          foundSheetId
        }

    val batchUpdateRequest =
      BatchUpdateSpreadsheetRequest()
        .setRequests(
          generateUnmergeRequests(relevantSheetId) +
            generateUpdateRequests(table.cells, relevantSheetId) +
            generateResizeRequests(table.columnWidths, relevantSheetId) +
            generateMergeRequests(table.cells, relevantSheetId)
        )

    clearSheet(spreadsheet.spreadsheetId, sheetName).bind()

    runCatching {
        apiClient.spreadsheets().batchUpdate(courseSpreadsheetId, batchUpdateRequest).execute()
      }
      .mapError { BatchUpdateError(courseSpreadsheetId, it) }
      .bind()
  }

  private fun BindingScope<EduPlatformError>.createQuizzesSheet(courseSpreadsheetId: String) {
    val batchUpdateRequest =
      BatchUpdateSpreadsheetRequest().setRequests(createAddNewSheetRequest(QUIZZES_SHEET_TITLE))

    runCatching {
        apiClient.spreadsheets().batchUpdate(courseSpreadsheetId, batchUpdateRequest).execute()
      }
      .mapError { CreateSheetError(QUIZZES_SHEET_TITLE, it) }
      .bind()
  }

  private fun clearSheet(spreadsheetId: String, sheetName: String): Result<Unit, EduPlatformError> =
    runCatching {
        apiClient
          .spreadsheets()
          .values()
          .clear(spreadsheetId, sheetName, ClearValuesRequest())
          .execute()
        Unit
      }
      .mapError { ClearSheetError(spreadsheetId, sheetName, it) }

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
