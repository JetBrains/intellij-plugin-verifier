/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.ide.diff.builder.persistence.json

import com.jetbrains.plugin.structure.base.utils.createDir
import com.jetbrains.plugin.structure.base.utils.deleteLogged
import com.jetbrains.plugin.structure.base.utils.extension
import kotlinx.serialization.stringify
import org.jetbrains.ide.diff.builder.api.ApiReport
import org.jetbrains.ide.diff.builder.persistence.ApiReportWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

class JsonApiReportWriter : ApiReportWriter {
  override fun saveReport(apiReport: ApiReport, reportPath: Path) {
    require(reportPath.extension == "json")
    reportPath.deleteLogged()
    reportPath.parent.createDir()
    val json = jsonInstance.encodeToString(ApiReport.serializer(), apiReport)
    Files.newBufferedWriter(
      reportPath,
      StandardOpenOption.CREATE,
      StandardOpenOption.WRITE
    ).use { writer ->
      writer.write(json)
    }
  }
}