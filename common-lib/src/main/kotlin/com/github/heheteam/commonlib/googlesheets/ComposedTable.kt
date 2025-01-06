package com.github.heheteam.commonlib.googlesheets

internal data class ComposedTable(
  val cells: List<List<FormattedCell>>,
  val columnWidths: List<Int?>,
)
