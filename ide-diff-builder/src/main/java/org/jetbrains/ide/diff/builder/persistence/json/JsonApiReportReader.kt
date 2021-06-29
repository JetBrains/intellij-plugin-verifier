/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.ide.diff.builder.persistence.json

import org.jetbrains.ide.diff.builder.api.ApiReport
import org.jetbrains.ide.diff.builder.persistence.ApiReportReader
import java.nio.file.Files
import java.nio.file.Path

class JsonApiReportReader : ApiReportReader {
  override fun readApiReport(reportPath: Path): ApiReport =
    jsonInstance.decodeFromString(
      ApiReport.serializer(),
      Files.newBufferedReader(reportPath).use { it.readText() }
    )
}