/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.output.teamcity

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class TeamCityLogTest {

  @Test
  fun `publishes artifact path specification`() {
    val output = ByteArrayOutputStream()
    val log = TeamCityLog(PrintStream(output))

    log.publishArtifacts("verification-report/internal-api-usages-summary.json => .")

    assertEquals(
      "##teamcity[publishArtifacts 'verification-report/internal-api-usages-summary.json => .']\n",
      output.toString()
    )
  }
}
