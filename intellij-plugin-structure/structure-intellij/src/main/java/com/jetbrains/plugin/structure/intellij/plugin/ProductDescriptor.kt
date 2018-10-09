package com.jetbrains.plugin.structure.intellij.plugin

import java.time.LocalDate

data class ProductDescriptor(val code: String, val releaseDate: LocalDate, val releaseVersion: Int)