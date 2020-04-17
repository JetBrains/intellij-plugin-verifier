/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.ide.diff.builder.persistence

import org.jetbrains.ide.diff.builder.api.ApiReport
import java.nio.file.Path

interface ApiReportWriter {
  fun saveReport(apiReport: ApiReport, reportPath: Path)
}