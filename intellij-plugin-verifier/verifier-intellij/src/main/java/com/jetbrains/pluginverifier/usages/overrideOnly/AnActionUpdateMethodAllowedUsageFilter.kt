package com.jetbrains.pluginverifier.usages.overrideOnly

import com.jetbrains.pluginverifier.verifiers.filter.ApiUsageFilter


typealias BinaryClassName = String

val anActionUpdateMethodDescriptor = MethodDescriptor("update", "(Lcom/intellij/openapi/actionSystem/AnActionEvent;)V")
const val anActionClass: BinaryClassName = "com/intellij/openapi/actionSystem/AnAction"

class AnActionUpdateMethodAllowedUsageFilter(
  private val delegate: ApiUsageFilter
  = OverrideOnlyMethodAllowedUsageFilter()) : ApiUsageFilter by delegate