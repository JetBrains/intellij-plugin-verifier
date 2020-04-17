/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.results.hierarchy

data class ClassHierarchy(
  val name: String,
  val isInterface: Boolean,
  var superClass: ClassHierarchy?,
  var superInterfaces: List<ClassHierarchy>
)