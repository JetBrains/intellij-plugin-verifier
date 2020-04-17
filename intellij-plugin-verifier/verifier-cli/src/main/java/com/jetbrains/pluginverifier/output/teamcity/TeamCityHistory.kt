/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.output.teamcity

import com.google.gson.Gson
import com.jetbrains.plugin.structure.base.utils.readText
import com.jetbrains.plugin.structure.base.utils.writeText
import java.nio.file.Path

data class TeamCityTest(val suiteName: String, val testName: String)

data class TeamCityHistory(val tests: List<TeamCityTest>) {

  companion object {

    private val json = Gson()

    fun readFromFile(file: Path): TeamCityHistory =
      json.fromJson(file.readText(), TeamCityHistory::class.java)
  }

  fun writeToFile(file: Path) {
    file.writeText(json.toJson(this))
  }

  fun reportOldSkippedTestsSuccessful(
    previousTests: TeamCityHistory,
    tc: TeamCityLog
  ) {
    val skippedTests = previousTests.tests - tests
    for ((suiteName, tests) in skippedTests.groupBy { it.suiteName }) {
      tc.testSuiteStarted(suiteName).use {
        for (test in tests) {
          tc.testStarted(test.testName).close()
        }
      }
    }
  }

}
