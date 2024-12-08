import com.github.heheteam.commonlib.*
import com.github.heheteam.commonlib.api.ProblemId
import com.github.heheteam.commonlib.api.StudentId
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.*
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials

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
    )
      .setApplicationName("Google Sheets API Demo")
      .build()
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

    val sortedProblems = problems.sortedWith(compareBy<Problem> { it.assignmentId.id }.thenBy { it.number })
    val sortedAssignments = assignments.sortedWith(compareBy { it.id.id })
    val assignmentIds = assignments.associateBy { it.id }
    val assignmentSizes = assignments.associate { it.id to 0 }.toMutableMap()

    val data: List<List<Any>> = listOf(
      listOf("", "", "") + sortedProblems.map {
        assignmentSizes[it.assignmentId] = assignmentSizes[it.assignmentId]?.let { i -> i + 1 } ?: 1
        assignmentIds[it.assignmentId]?.description ?: ""
      },
      listOf("id", "surname", "name") + sortedProblems.map { it.number },
    ) + students.map { student ->
      listOf(student.id.id, student.surname, student.name) +
        sortedProblems.map { problem -> performance[student.id]?.get(problem.id) ?: "" }
    }

    val updateRequests = data.mapIndexed { rowIndex, row ->
      val rowData = RowData().setValues(
        row.mapIndexed { colIndex, cellValue ->
          // Create a CellData object for each cell in the row
          CellData().setUserEnteredValue(
            ExtendedValue().setStringValue(cellValue.toString()),
          )
        },
      )

      Request().setUpdateCells(
        UpdateCellsRequest()
          .setRange(
            GridRange()
              .setSheetId(sheetId)
              .setStartRowIndex(rowIndex)
              .setEndRowIndex(rowIndex + 1)
              .setStartColumnIndex(0)
              .setEndColumnIndex(row.size),
          )
          .setRows(listOf(rowData))
          .setFields("userEnteredValue"),
      )
    }

    var endColumn = 3
    val mergeCellsRequests = sortedAssignments.map {
      val startColumn = endColumn
      endColumn += assignmentSizes[it.id] ?: 0
      Request().setMergeCells(
        MergeCellsRequest()
          .setMergeType("MERGE_ALL")
          .setRange(
            GridRange().setSheetId(sheetId)
              .setStartRowIndex(0).setEndRowIndex(1)
              .setStartColumnIndex(startColumn).setEndColumnIndex(endColumn),
          ),
      )
    }

    val batchUpdateRequest = BatchUpdateSpreadsheetRequest()
      .setRequests(mergeCellsRequests + updateRequests)

    apiClient.spreadsheets().batchUpdate(spreadsheetId, batchUpdateRequest).execute()
  }
}
