package com.jetbrains.plugin.structure.domain

import com.jetbrains.plugin.structure.dotnet.version.VersionMatching
import org.junit.Assert
import org.junit.Test

class ReSharperRangeTest {
  @Test
  fun getReSharperRangeFromWaveStringOnlyMinTest() {
    val reSharperRange = VersionMatching.getResharperRangeFromWaveRangeString("4.0")
    Assert.assertTrue(reSharperRange.isMinIncluded)
    Assert.assertEquals(10, reSharperRange.min?.baseline)
    Assert.assertEquals(0, reSharperRange.min?.build)
    Assert.assertNull(reSharperRange.min?.minor)
    Assert.assertNull(reSharperRange.max)
  }

  @Test
  fun getReSharperRangeFromStringOnlyMinExclusiveTest() {
    val reSharperRange = VersionMatching.getResharperRangeFromWaveRangeString("(1.0,)")
    Assert.assertFalse(reSharperRange.isMinIncluded)
    Assert.assertEquals(9, reSharperRange.min?.baseline)
    Assert.assertEquals(0, reSharperRange.min?.build)
    Assert.assertNull(reSharperRange.min?.minor)
    Assert.assertNull(reSharperRange.max)
  }

  @Test
  fun getWaveRangeFromStringOnlyMinInclusiveTest() {
    val reSharperRange = VersionMatching.getResharperRangeFromWaveRangeString("[183.1,)")
    Assert.assertTrue(reSharperRange.isMinIncluded)
    Assert.assertEquals(2018, reSharperRange.min?.baseline)
    Assert.assertEquals(3, reSharperRange.min?.build)
    Assert.assertEquals(1, reSharperRange.min?.minor)
    Assert.assertNull(reSharperRange.max)
  }

  @Test
  fun getWaveRangeFromStringExactVersionMatchTest() {
    val reSharperRange = VersionMatching.getResharperRangeFromWaveRangeString("[213.0.0-eap01]")
    Assert.assertTrue(reSharperRange.isMinIncluded)
    Assert.assertEquals(2021, reSharperRange.min?.baseline)
    Assert.assertEquals(3, reSharperRange.min?.build)
    Assert.assertNull(reSharperRange.min?.minor)
    Assert.assertTrue(reSharperRange.isMaxIncluded)
    Assert.assertEquals(2021, reSharperRange.max?.baseline)
    Assert.assertEquals(3, reSharperRange.max?.build)
    Assert.assertNull(reSharperRange.max?.minor)
  }

  @Test
  fun getWaveRangeFromStringOnlyMaxInclusiveTest() {
    val reSharperRange = VersionMatching.getResharperRangeFromWaveRangeString("(,9.1.0]")
    Assert.assertFalse(reSharperRange.isMinIncluded)
    Assert.assertTrue(reSharperRange.isMaxIncluded)
    Assert.assertEquals(2017, reSharperRange.max?.baseline)
    Assert.assertEquals(2, reSharperRange.max?.build)
    Assert.assertEquals(1, reSharperRange.max?.minor)
  }

  @Test
  fun getWaveRangeFromStringOnlyMaxExclusiveTest() {
    val reSharperRange = VersionMatching.getResharperRangeFromWaveRangeString("(,12.1.0)")
    Assert.assertFalse(reSharperRange.isMinIncluded)
    Assert.assertFalse(reSharperRange.isMaxIncluded)
    Assert.assertEquals(2018, reSharperRange.max?.baseline)
    Assert.assertEquals(1, reSharperRange.max?.build)
    Assert.assertEquals(1, reSharperRange.max?.minor)
  }

  @Test
  fun getWaveRangeFromStringExactRangeInclusiveTest() {
    val reSharperRange = VersionMatching.getResharperRangeFromWaveRangeString("[202.0.0, 212.0.0]")
    Assert.assertTrue(reSharperRange.isMinIncluded)
    Assert.assertEquals(2020, reSharperRange.min?.baseline)
    Assert.assertEquals(2, reSharperRange.min?.build)
    Assert.assertNull(reSharperRange.min?.minor)
    Assert.assertTrue(reSharperRange.isMaxIncluded)
    Assert.assertEquals(2021, reSharperRange.max?.baseline)
    Assert.assertEquals(2, reSharperRange.max?.build)
    Assert.assertNull(reSharperRange.max?.minor)
  }

  @Test
  fun getWaveRangeFromStringExactRangeExclusiveTest() {
    val reSharperRange = VersionMatching.getResharperRangeFromWaveRangeString("(6.1.0, 8.0.0)")
    Assert.assertFalse(reSharperRange.isMinIncluded)
    Assert.assertEquals(2016, reSharperRange.min?.baseline)
    Assert.assertEquals(2, reSharperRange.min?.build)
    Assert.assertEquals(1, reSharperRange.min?.minor)
    Assert.assertFalse(reSharperRange.isMaxIncluded)
    Assert.assertEquals(2017, reSharperRange.max?.baseline)
    Assert.assertEquals(1, reSharperRange.max?.build)
    Assert.assertNull(reSharperRange.max?.minor)
  }

  @Test
  fun getWaveRangeFromStringInclusiveMinExclusiveMaxTest() {
    val reSharperRange = VersionMatching.getResharperRangeFromWaveRangeString("[203.0.0-eap08, 203.9999.0)")
    Assert.assertTrue(reSharperRange.isMinIncluded)
    Assert.assertEquals(2020, reSharperRange.min?.baseline)
    Assert.assertEquals(3, reSharperRange.min?.build)
    Assert.assertNull(reSharperRange.min?.minor)
    Assert.assertFalse(reSharperRange.isMaxIncluded)
    Assert.assertEquals(2020, reSharperRange.max?.baseline)
    Assert.assertEquals(3, reSharperRange.max?.build)
    Assert.assertEquals(9999, reSharperRange.max?.minor)
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
}