/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.reporting

import java.io.Closeable

/**
 * Reporter is responsible for providing intermediate and
 * end results of a process to interested clients.
 */
interface Reporter<in T> : Closeable {
  fun report(t: T)
}