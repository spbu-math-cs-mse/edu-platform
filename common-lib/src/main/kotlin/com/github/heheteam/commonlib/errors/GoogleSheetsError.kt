package com.github.heheteam.commonlib.errors

data class CreateSpreadsheetError(
  val courseName: String,
  val exception: Throwable,
  override val causedBy: EduPlatformError? = null,
) : EduPlatformError {
  override val shortDescription: String =
    "Error: cannot create spreadsheet for course: \"$courseName\". Reason: ${exception.message}"
}

data class SetupPermissionsError(
  val courseName: String,
  val exception: Throwable,
  override val causedBy: EduPlatformError? = null,
) : EduPlatformError {
  override val shortDescription: String =
    "Error: cannot set up permissions for access to the course spreadsheet: \"$courseName\". " +
      "Reason: ${exception.message}"
}

data class CreateSheetError(
  val courseName: String,
  val exception: Throwable,
  override val causedBy: EduPlatformError? = null,
) : EduPlatformError {
  override val shortDescription: String =
    "Error: cannot create a sheet for course: \"$courseName\". " + "Reason: ${exception.message}"
}

data class GetSpreadsheetError(
  val spreadsheetId: String,
  val exception: Throwable,
  override val causedBy: EduPlatformError? = null,
) : EduPlatformError {
  override val shortDescription: String =
    "Error: cannot get spreadsheet with ID: \"$spreadsheetId\". " + "Reason: ${exception.message}"
}

data class SheetNotFoundError(
  val spreadsheetId: String,
  val sheetName: String,
  override val causedBy: EduPlatformError? = null,
) : EduPlatformError {
  override val shortDescription: String =
    "Error: sheet '${sheetName}' not found in spreadsheet with ID: $spreadsheetId"
}

data class ClearSheetError(
  val spreadsheetId: String,
  val sheetName: String,
  val exception: Throwable,
  override val causedBy: EduPlatformError? = null,
) : EduPlatformError {
  override val shortDescription: String =
    "Error: cannot clear sheet '${sheetName}' in spreadsheet with ID: $spreadsheetId. " +
      "Reason: ${exception.message}"
}

data class BatchUpdateError(
  val spreadsheetId: String,
  val exception: Throwable,
  override val causedBy: EduPlatformError? = null,
) : EduPlatformError {
  override val shortDescription: String =
    "Error: failed to update spreadsheet with ID: $spreadsheetId. " + "Reason: ${exception.message}"
}
