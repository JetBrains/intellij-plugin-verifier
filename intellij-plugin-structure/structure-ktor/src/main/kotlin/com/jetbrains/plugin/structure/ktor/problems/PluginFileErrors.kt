/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ktor.problems

import com.jetbrains.plugin.structure.base.problems.IncorrectPluginFile
import com.jetbrains.plugin.structure.base.problems.PluginFileError
import com.jetbrains.plugin.structure.ktor.KtorFeaturePluginManager

fun createIncorrectKtorFeatureFile(fileName: String): PluginFileError =
  IncorrectPluginFile(fileName, ".zip archive with ${KtorFeaturePluginManager.DESCRIPTOR_NAME} file.")