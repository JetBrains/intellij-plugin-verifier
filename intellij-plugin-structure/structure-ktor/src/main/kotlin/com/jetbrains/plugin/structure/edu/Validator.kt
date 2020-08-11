/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.edu

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.problems.PropertyNotSpecified
import com.jetbrains.plugin.structure.edu.bean.KtorFeatureDescriptor
import com.jetbrains.plugin.structure.edu.bean.VENDOR

internal fun validateEduPluginBean(descriptor: KtorFeatureDescriptor): List<PluginProblem> {
  val problems = mutableListOf<PluginProblem>()
  val vendor = descriptor.vendor
  if (vendor == null || vendor.name.isNullOrBlank()) {
    problems.add(PropertyNotSpecified(VENDOR))
  }
  // TODO add field validation
  return problems
}
