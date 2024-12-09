package googlesheets

import com.google.api.services.sheets.v4.model.*

enum class DataType {
  STRING,
  LONG,
  DOUBLE,
  FORMULA,
  BOOL,
  ERROR,
}

internal class FancyCell(
  data: String = "",
  dataType: DataType = DataType.STRING,
  val width: Int = 1,
) {
  private val cellData : CellData = CellData().setUserEnteredValue(parse(data, dataType))
  private val cellFormat = CellFormat()
  private val borders: Borders = Borders()

  fun bold(): FancyCell {
    cellData.setUserEnteredFormat(cellFormat.setTextFormat(TextFormat().setBold(true)))
    return this
  }

  fun borders(): FancyCell {
    borders.setTop(Border().apply {width = 1 ; style = "STROKE"})
    return this
  }

  fun centerAlign(): FancyCell {
    cellData.setUserEnteredFormat(cellFormat.setHorizontalAlignment("CENTER"))
    return this
  }

 private fun parse(data: String, dataType: DataType): ExtendedValue =
        when (dataType) {
          DataType.STRING -> ExtendedValue().setStringValue(data)
          DataType.LONG -> ExtendedValue().setNumberValue(data.toLongOrNull()?.toDouble())
          DataType.DOUBLE -> ExtendedValue().setNumberValue(data.toDoubleOrNull())
          DataType.FORMULA -> ExtendedValue().setFormulaValue(data)
          DataType.BOOL -> ExtendedValue().setBoolValue(data.toBooleanStrictOrNull())
          DataType.ERROR -> ExtendedValue().setErrorValue(ErrorValue().setMessage(data))
        }

  fun toCellData(): CellData = cellData
}
