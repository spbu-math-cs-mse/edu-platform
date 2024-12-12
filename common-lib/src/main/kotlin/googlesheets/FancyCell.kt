package com.github.heheteam.commonlib.googlesheets

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
  private val cellFormat = CellFormat()
    .setTextFormat(TextFormat().setBold(false))
    .setBorders(Borders())

  private val cellData: CellData = CellData()
    .setUserEnteredValue(parse(data, dataType))
    .setUserEnteredFormat(cellFormat)

  fun bold(): FancyCell {
    cellData.setUserEnteredFormat(cellFormat.setTextFormat(TextFormat().setBold(true)))
    return this
  }

  fun borders(width: Int = 1): FancyCell {
    cellFormat.apply {
      borders.setTop(Border().setWidth(width).setStyle("SOLID"))
      borders.setBottom(Border().setWidth(width).setStyle("SOLID"))
      borders.setLeft(Border().setWidth(width).setStyle("SOLID"))
      borders.setRight(Border().setWidth(width).setStyle("SOLID"))
    }
    return this
  }

  fun rightBorder(width: Int = 1): FancyCell {
    cellFormat.apply {
      borders.setRight(Border().setWidth(width).setStyle("SOLID"))
    }
    return this
  }

  fun leftBorder(width: Int = 1): FancyCell {
    cellFormat.apply {
      borders.setLeft(Border().setWidth(width).setStyle("SOLID"))
    }
    return this
  }

  fun topBorder(width: Int = 1): FancyCell {
    cellFormat.apply {
      borders.setTop(Border().setWidth(width).setStyle("SOLID"))
    }
    return this
  }

  fun bottomBorder(width: Int = 1): FancyCell {
    cellFormat.apply {
      borders.setBottom(Border().setWidth(width).setStyle("SOLID"))
    }
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
