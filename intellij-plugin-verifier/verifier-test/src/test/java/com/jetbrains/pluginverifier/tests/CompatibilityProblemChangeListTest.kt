/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tests

import com.jetbrains.pluginverifier.analysis.CompatibilityProblemChangeList
import com.jetbrains.pluginverifier.results.problems.PackageNotFoundProblem
import org.junit.Assert.assertEquals
import org.junit.Test

class CompatibilityProblemChangeListTest {
  @Test
  fun `problems are added and removed`() {
    val pluginPackageNotFoundProblem = PackageNotFoundProblem("com.intellij.json", emptySet())
    val otherPackageNotFoundProblem = PackageNotFoundProblem("com.intellij.ide", emptySet())
    val changeList = CompatibilityProblemChangeList()
    changeList += otherPackageNotFoundProblem
    changeList -= pluginPackageNotFoundProblem

    assertEquals(1, changeList.problems.size)
    assertEquals(1, changeList.addedProblems.size)
    assertEquals(1, changeList.removedProblems.size)
  }

  @Test
  fun `same problems are not duplicated`() {
    val pluginPackageNotFoundProblem = PackageNotFoundProblem("com.intellij.json", emptySet())
    val pluginPackageNotFoundProblemDuplicate = PackageNotFoundProblem("com.intellij.json", emptySet())

    val changeList = CompatibilityProblemChangeList()
    changeList += pluginPackageNotFoundProblem
    changeList += pluginPackageNotFoundProblemDuplicate

    assertEquals(1, changeList.problems.size)
    assertEquals(1, changeList.addedProblems.size)
    assertEquals(0, changeList.removedProblems.size)
  }
}