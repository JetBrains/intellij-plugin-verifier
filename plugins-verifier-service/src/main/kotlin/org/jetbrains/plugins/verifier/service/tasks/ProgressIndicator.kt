/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.plugins.verifier.service.tasks

/**
 * Progress indicator is used to track task execution progress.
 */
data class ProgressIndicator(
  @Volatile var fraction: Double = 0.0,
  @Volatile var text: String = ""
)