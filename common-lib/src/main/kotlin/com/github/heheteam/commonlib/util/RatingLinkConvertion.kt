package com.github.heheteam.commonlib.util

import com.github.heheteam.commonlib.interfaces.SpreadsheetId

fun SpreadsheetId.toUrl(): String = "https://docs.google.com/spreadsheets/d/$long/"
