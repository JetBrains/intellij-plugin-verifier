package com.jetbrains.plugin.structure.base.utils

import java.io.File

fun File.isZip(): Boolean = this.hasExtension("zip")

fun File.isJar(): Boolean = this.hasExtension("jar")

fun File.hasExtension(expected: String) =
    isFile && expected == extension