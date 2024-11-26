package com.github.heheteam.commonlib.database

fun String.toLongIdHack(): Long = this.hashCode().toLong()
