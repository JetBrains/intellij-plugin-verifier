/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.intellij.plugin.enums.CpuArch
import com.jetbrains.plugin.structure.intellij.plugin.enums.OS

/**
 * Calculates the operating system and cpu architecture constraints based on the `com.intellij.modules.os.*` and
 * `com.intellij.modules.arch.*` non-optional modules. If the collection is empty, no restriction is applied on the
 * Marketplace side.
 */
val IdePlugin.osConstraints: Set<OS>
  get() = dependencies.filter { it.isOptional.not() }.mapNotNullTo(LinkedHashSet()) { OS.getByModule(it.id) }
val IdePlugin.archConstraints: Set<CpuArch>
  get() = dependencies.filter { it.isOptional.not() }.mapNotNullTo(LinkedHashSet()) { CpuArch.getByModule(it.id) }