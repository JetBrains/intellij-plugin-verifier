/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.verifiers.filter

import com.jetbrains.pluginverifier.verifiers.hasAnnotation
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFile

/**
 * Verification filter that excludes classes marked with `com.intellij.ide.plugins.DynamicallyLoaded`]` annotation.
 * This annotation indicates that the class is loaded by custom class loader, thus
 * it may be impossible to statically analyse its bytecode.
 */
class DynamicallyLoadedFilter : ClassFilter {

  companion object {
    private const val DYNAMICALLY_LOADED = "com/intellij/ide/plugins/DynamicallyLoaded"
  }

  override fun shouldVerify(classFile: ClassFile) =
    !classFile.annotations.hasAnnotation(DYNAMICALLY_LOADED)

}