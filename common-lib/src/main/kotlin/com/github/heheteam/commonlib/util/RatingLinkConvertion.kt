package com.github.heheteam.commonlib.util

import com.github.heheteam.commonlib.api.SpreadsheetId

fun SpreadsheetId.toUrl(): String = "https://docs.google.com/spreadsheets/d/$id/"
