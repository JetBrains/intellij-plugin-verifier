package com.jetbrains.plugin.structure.domain

import com.jetbrains.plugin.structure.dotnet.version.VersionMatching
import org.junit.Assert
import org.junit.Test

class ReSharperRangeTest {
  @Test
  fun getReSharperRangeFromWaveStringOnlyMinTest() {
    val reSharperRange = VersionMatching.getResharperRangeFromWaveRangeString("4.0")
    Assert.assertTrue(reSharperRange.isMinIncluded)
    Assert.assertEquals(listOf(10, 0), reSharperRange.min?.components)
    Assert.assertNull(reSharperRange.max)
  }

  @Test
  fun getReSharperRangeFromStringOnlyMinExclusiveTest() {
    val reSharperRange = VersionMatching.getResharperRangeFromWaveRangeString("(1.0,)")
    Assert.assertFalse(reSharperRange.isMinIncluded)
    Assert.assertEquals(listOf(9, 0), reSharperRange.min?.components)
    Assert.assertNull(reSharperRange.max)
  }

  @Test
  fun getWaveRangeFromStringOnlyMinInclusiveTest() {
    val reSharperRange = VersionMatching.getResharperRangeFromWaveRangeString("[183.1,)")
    Assert.assertTrue(reSharperRange.isMinIncluded)
    Assert.assertEquals(listOf(2018, 3, 1), reSharperRange.min?.components)
    Assert.assertNull(reSharperRange.max)
  }

  @Test
  fun getWaveRangeFromStringExactVersionMatchTest() {
    val reSharperRange = VersionMatching.getResharperRangeFromWaveRangeString("[213.0.0-eap01]")
    Assert.assertTrue(reSharperRange.isMinIncluded)
    Assert.assertEquals(listOf(2021, 3), reSharperRange.min?.components)
    Assert.assertTrue(reSharperRange.isMaxIncluded)
    Assert.assertEquals(listOf(2021, 3), reSharperRange.max?.components)
  }

  @Test(expected = IllegalArgumentException::class)
  fun getWaveRangeFromStringNonexistentWaveVersionsWithoutReplacementTest() {
    VersionMatching.getResharperRangeFromWaveRangeString("[10.0.0]")
  }

  @Test
  fun getWaveRangeFromStringNonexistentWaveVersionsWithReplacementTest() {
    val reSharperRange = VersionMatching.getResharperRangeFromWaveRangeString("[10.0.0, 13.0.0]")
    Assert.assertTrue(reSharperRange.isMinIncluded)
    Assert.assertEquals(listOf(2017, 3), reSharperRange.min?.components)
    Assert.assertTrue(reSharperRange.isMinIncluded)
    Assert.assertFalse(reSharperRange.isMaxIncluded)
    Assert.assertEquals(listOf(2018, 2), reSharperRange.max?.components)
  }

  @Test
  fun getWaveRangeFromStringOnlyMaxInclusiveTest() {
    val reSharperRange = VersionMatching.getResharperRangeFromWaveRangeString("(,9.1.0]")
    Assert.assertFalse(reSharperRange.isMinIncluded)
    Assert.assertTrue(reSharperRange.isMaxIncluded)
    Assert.assertEquals(listOf(2017, 2, 1), reSharperRange.max?.components)
  }

  @Test
  fun getWaveRangeFromStringOnlyMaxExclusiveTest() {
    val reSharperRange = VersionMatching.getResharperRangeFromWaveRangeString("(,12.1.0)")
    Assert.assertFalse(reSharperRange.isMinIncluded)
    Assert.assertFalse(reSharperRange.isMaxIncluded)
    Assert.assertEquals(listOf(2018, 1, 1), reSharperRange.max?.components)
  }

  @Test
  fun getWaveRangeFromStringExactRangeInclusiveTest() {
    val reSharperRange = VersionMatching.getResharperRangeFromWaveRangeString("[202.0.0, 212.0.0]")
    Assert.assertTrue(reSharperRange.isMinIncluded)
    Assert.assertEquals(listOf(2020, 2), reSharperRange.min?.components)
    Assert.assertTrue(reSharperRange.isMaxIncluded)
    Assert.assertEquals(listOf(2021, 2), reSharperRange.max?.components)
  }

  @Test
  fun getWaveRangeFromStringExactRangeExclusiveTest() {
    val reSharperRange = VersionMatching.getResharperRangeFromWaveRangeString("(6.1.0, 8.0.0)")
    Assert.assertFalse(reSharperRange.isMinIncluded)
    Assert.assertEquals(listOf(2016, 2, 1), reSharperRange.min?.components)
    Assert.assertFalse(reSharperRange.isMaxIncluded)
    Assert.assertEquals(listOf(2017, 1), reSharperRange.max?.components)
  }

  @Test
  fun getWaveRangeFromStringInclusiveMinExclusiveMaxTest() {
    val reSharperRange = VersionMatching.getResharperRangeFromWaveRangeString("[203.0.0-eap08, 203.9999.0)")
    Assert.assertTrue(reSharperRange.isMinIncluded)
    Assert.assertEquals(listOf(2020, 3), reSharperRange.min?.components)
    Assert.assertFalse(reSharperRange.isMaxIncluded)
    Assert.assertEquals(listOf(2020, 3, 9999), reSharperRange.max?.components)
  }

  @Test(expected = IllegalArgumentException::class)
  fun minVersionIsGreaterThanMaxTest() {
    VersionMatching.getResharperRangeFromWaveRangeString("[203.1.0, 203.0.0)")
  }

  @Test(expected = IllegalArgumentException::class)
  fun minMaxTheSameExclusiveTest() {
    VersionMatching.getResharperRangeFromWaveRangeString("(211.3.0, 211.3.0)")
  }

  @Test(expected = IllegalArgumentException::class)
  fun severalPartsInRangeTest() {
    VersionMatching.getResharperRangeFromWaveRangeString("[183.1, 201.0, 211.3.0)")
  }

  @Test(expected = IllegalArgumentException::class)
  fun oneNumberInRangeExclusiveTest() {
    VersionMatching.getResharperRangeFromWaveRangeString("(4.0)")
  }

  @Test
  fun getReSharperRangeFromReSharperString() {
    val reSharperRange = VersionMatching.getReSharperRangeFromString("[8.2, 8.3)")
    Assert.assertTrue(reSharperRange.isMinIncluded)
    Assert.assertEquals(listOf(8, 2), reSharperRange.min?.components)
    Assert.assertFalse(reSharperRange.isMaxIncluded)
    Assert.assertEquals(listOf(8, 3), reSharperRange.max?.components)
  }
}