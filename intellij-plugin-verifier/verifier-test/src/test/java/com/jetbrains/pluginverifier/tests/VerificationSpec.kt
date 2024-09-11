package com.jetbrains.pluginverifier.tests

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.filtering.ApiUsageFilter
import com.jetbrains.pluginverifier.filtering.ProblemsFilter

class VerificationSpec {
  lateinit var ide: Ide
  lateinit var plugin: IdePlugin
  var problemsFilters: List<ProblemsFilter> = emptyList()
  var apiUsageFilters: List<ApiUsageFilter> = emptyList()
  var kotlin: Boolean = false
}