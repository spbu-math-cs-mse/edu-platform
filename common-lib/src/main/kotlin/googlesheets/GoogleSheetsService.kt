import com.github.heheteam.commonlib.*
import com.github.heheteam.commonlib.api.AssignmentId
import com.github.heheteam.commonlib.api.ProblemId
import com.github.heheteam.commonlib.api.StudentId
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.*
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import googlesheets.DataType
import googlesheets.FancyCell

class GoogleSheetsService(serviceAccountKeyFile: String, private val spreadsheetId: String) {
  private val apiClient: Sheets

  init {
    val credentials =
      GoogleCredentials.fromStream(object {}.javaClass.classLoader.getResourceAsStream(serviceAccountKeyFile))
        .createScoped(listOf("https://www.googleapis.com/auth/spreadsheets"))

    apiClient = Sheets.Builder(
      com.google.api.client.http.javanet.NetHttpTransport(),
      com.google.api.client.json.gson.GsonFactory.getDefaultInstance(),
      HttpCredentialsAdapter(credentials),
    ).setApplicationName("GoogleSheetsService").build()
  }

  private fun createCourseSheet(course: Course) {
    val addSheetRequest = AddSheetRequest().setProperties(
      com.google.api.services.sheets.v4.model.SheetProperties().setTitle(course.name),
    )

    val batchUpdateRequest = BatchUpdateSpreadsheetRequest().setRequests(
      listOf(Request().setAddSheet(addSheetRequest)),
    )

    apiClient.spreadsheets()
      .batchUpdate(spreadsheetId, batchUpdateRequest)
      .execute()
  }

  fun updateRating(
    course: Course,
    assignments: List<Assignment>,
    problems: List<Problem>,
    students: List<Student>,
    performance: Map<StudentId, Map<ProblemId, Grade>>,
  ) {
    var spreadsheet = apiClient.spreadsheets().get(spreadsheetId).execute()
    val sheetNames = spreadsheet.sheets.map { it.properties.title }
    if (course.name !in sheetNames) {
      createCourseSheet(course)
      spreadsheet = apiClient.spreadsheets().get(spreadsheetId).execute()
    }
    val sheetId = spreadsheet.sheets.first { it.properties.title == course.name }.properties.sheetId
    val data: List<List<FancyCell>> = composeTable(problems, assignments, students, performance)

    val batchUpdateRequest = BatchUpdateSpreadsheetRequest().setRequests(
      generateUpdateRequests(data, sheetId)+
      generateMergeRequests(data, sheetId)
    )

    apiClient.spreadsheets().batchUpdate(spreadsheetId, batchUpdateRequest).execute()
  }

  private fun generateMergeRequests(
    data: List<List<FancyCell>>,
    sheetId: Int?,
  ): List<Request> = data.mapIndexed { row, rowList ->
    var column = 0
    rowList.mapNotNull { cell ->
      var request: Request? = null
      if (cell.width > 1) {
        request = Request().setMergeCells(
          MergeCellsRequest()
            .setMergeType("MERGE_ALL")
            .setRange(gridRange(sheetId, row, row, column, column + cell.width)),
        )
      }
      column += cell.width
      return@mapNotNull request
    }
  }.flatten().toList()

  private fun generateUpdateRequests(
    data: List<List<FancyCell>>,
    sheetId: Int?,
  ): List<Request> = data.flatMapIndexed { rowIndex, row ->
    val rowExtended = row.flatMap { cell ->
      List(cell.width) {
        cell
      }
    }

    listOf(
      Request().setUpdateCells(
        UpdateCellsRequest()
          .setRange(gridRange(sheetId, rowIndex, rowIndex + 1, 0, row.fold(0) { acc, cell -> acc + cell.width }))
          .setRows(listOf(RowData().setValues(rowExtended.map { it.toCellData() })))
          .setFields("userEnteredValue,userEnteredFormat.textFormat.bold,userEnteredFormat.horizontalAlignment"),
      ),
    )
  }

  private fun gridRange(sheetId: Int?, startRow: Int, endRow: Int, startColumn: Int, endColumn: Int): GridRange =
    GridRange().setSheetId(sheetId)
      .setStartRowIndex(startRow).setEndRowIndex(endRow + 1)
      .setStartColumnIndex(startColumn).setEndColumnIndex(endColumn)

  private fun composeTable(
    problems: List<Problem>,
    assignments: List<Assignment>,
    students: List<Student>,
    performance: Map<StudentId, Map<ProblemId, Grade>>,
  ): List<List<FancyCell>> {
    val sortedProblems = problems.sortedWith(compareBy<Problem> { it.assignmentId.id }.thenBy { it.number })
    val sortedAssignments = assignments.sortedWith(compareBy { it.id.id })
    val assignmentSizes = mutableMapOf<AssignmentId, Int>()

    for (problem in sortedProblems) {
      assignmentSizes[problem.assignmentId] = assignmentSizes[problem.assignmentId]?.let { i -> i + 1 } ?: 1
    }

    return listOf(
      // Row 1
      // 3 empty cells for id, name, surname
      List(3) { FancyCell() } +
        // Assignments
        sortedAssignments.map {
          FancyCell(it.description, DataType.STRING, assignmentSizes[it.id] ?: 0).bold().borders().centerAlign()
        },
      // Row 2
      listOf("id", "surname", "name").map { FancyCell(it, DataType.STRING).bold().borders() } +
        sortedProblems.map { FancyCell(it.number, DataType.STRING).bold() },
    ) +
      // Rows 3+
      students.map { student ->
        listOf(student.id.id, student.surname, student.name)
          .map { FancyCell(it.toString(), DataType.STRING) } +

          sortedProblems.map { problem -> performance[student.id]?.get(problem.id) ?: "" }
            .map { FancyCell(it.toString(), DataType.STRING) }
      }
  }
}
