package com.github.heheteam.commonlib.googlesheets

internal data class ComposedTable(
  val cells: List<List<FancyCell>>,
  val columnWidths: List<Int?>,
)
