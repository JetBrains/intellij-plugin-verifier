package com.jetbrains.plugin.structure.domain

import com.jetbrains.plugin.structure.dotnet.version.ReSharperVersion
import org.junit.Assert
import org.junit.Test

class ReSharperVersionTest {
  @Test
  fun versionWithoutProductCodeTest() {
    //test that no exception
    version("2021.3.2")
    version("2022.1.0.1")
    version("2021.1.5")
    version("9.1")
  }

  @Test
  fun versionWithProductCodeTest() {
    //test that no exception
    version("RSU-2021.3.2")
    version("RSCLT-2022.1.0.1")
  }

  @Test(expected = IllegalArgumentException::class)
  fun wrongBaselineVersionTest() {
    version("test.3.3")
  }

  @Test(expected = IllegalArgumentException::class)
  fun wrongBuildVersionTest() {
    version("138.test.3")
  }

  @Test(expected = IllegalArgumentException::class)
  fun wrongMinorVersionTest() {
    version("138.3.test")
  }

  @Test
  fun typicalVersionTest() {
    val resharperVersion = version("RSU-2021.3.1")
    Assert.assertEquals(2021, resharperVersion.components[0])
    Assert.assertEquals(3, resharperVersion.components[1])
    Assert.assertEquals(1, resharperVersion.components[2])
    Assert.assertEquals("RSU", resharperVersion.productCode)
    Assert.assertEquals("RSU-2021.3.1", resharperVersion.asString())
  }

  @Test
  fun versionWithOnly2ComponentsTest() {
    val resharperVersion = version("RSU-2021.3")
    Assert.assertEquals(2021, resharperVersion.components[0])
    Assert.assertEquals(3, resharperVersion.components[1])
    Assert.assertEquals(2, resharperVersion.components.size)
    Assert.assertEquals("RSU", resharperVersion.productCode)
    Assert.assertEquals("RSU-2021.3", resharperVersion.asString())
  }

  @Test
  fun versionWith4ComponentsTest() {
    val resharperVersion = version("RSU-2021.3")
    Assert.assertEquals(2021, resharperVersion.components[0])
    Assert.assertEquals(3, resharperVersion.components[1])
    Assert.assertEquals(2, resharperVersion.components.size)
    Assert.assertEquals("RSU", resharperVersion.productCode)
    Assert.assertEquals("RSU-2021.3", resharperVersion.asString())
  }

  @Test
  fun getLowerVersionOnlyBaselineTest() {
    val currentVersion = version("RSU-2023.0.0")
    val lowerVersion = currentVersion.getLowerVersion()
    Assert.assertEquals(2022, lowerVersion.components[0])
    Assert.assertEquals(9, lowerVersion.components[1])
    Assert.assertEquals(2, lowerVersion.components.size)
    Assert.assertEquals("RSU-2022.9", lowerVersion.asString())
  }

  @Test
  fun getLowerVersionBuildNotZeroTest() {
    val currentVersion = version("2021.2")
    val lowerVersion = currentVersion.getLowerVersion()
    Assert.assertEquals(2021, lowerVersion.components[0])
    Assert.assertEquals(1, lowerVersion.components[1])
    Assert.assertEquals(9, lowerVersion.components[2])
    Assert.assertEquals(3, lowerVersion.components.size)
    Assert.assertEquals("2021.1.9", lowerVersion.asString())
  }

  @Test
  fun getLowerVersionMinorNotZeroTest() {
    val currentVersion = version("RS-2022.3.1")
    val lowerVersion = currentVersion.getLowerVersion()
    Assert.assertEquals(2022, lowerVersion.components[0])
    Assert.assertEquals(3, lowerVersion.components[1])
    Assert.assertEquals(0, lowerVersion.components[2])
    Assert.assertEquals(3, lowerVersion.components.size)
    Assert.assertEquals("RS-2022.3.0", lowerVersion.asString())
  }

  @Test
  fun getHigherVersionOnlyBaselineTest() {
    val currentVersion = version("RSU-2023.0.0")
    val lowerVersion = currentVersion.getHigherVersion()
    Assert.assertEquals(2023, lowerVersion.components[0])
    Assert.assertEquals(0, lowerVersion.components[1])
    Assert.assertEquals(1, lowerVersion.components[2])
    Assert.assertEquals(3, lowerVersion.components.size)
    Assert.assertEquals("RSU-2023.0.1", lowerVersion.asString())
  }

  @Test
  fun getHigherVersionMaxMinorTest() {
    val maxInt = Int.MAX_VALUE
    val currentVersion = version("RSU-2024.0.$maxInt")
    val higherVersion = currentVersion.getHigherVersion()
    Assert.assertEquals(2024, higherVersion.components[0])
    Assert.assertEquals(1, higherVersion.components[1])
    Assert.assertEquals(2, higherVersion.components.size)
    Assert.assertEquals("RSU-2024.1", higherVersion.asString())
  }

  @Test
  fun getHigherVersionMaxBuildTest() {
    val maxInt = Int.MAX_VALUE
    val currentVersion = version("RS-2019.$maxInt.$maxInt")
    val higherVersion = currentVersion.getHigherVersion()
    Assert.assertEquals(2020, higherVersion.components[0])
    Assert.assertEquals(0, higherVersion.components[1])
    Assert.assertEquals(2, higherVersion.components.size)
    Assert.assertEquals("RS-2020.0", higherVersion.asString())
  }

  @Test(expected = IllegalArgumentException::class)
  fun getHigherVersionForMaxVersionTest() {
    val maxInt = Int.MAX_VALUE
    val currentVersion = version("RS-$maxInt.$maxInt.$maxInt")
    currentVersion.getHigherVersion()
  }

  private fun version(s: String): ReSharperVersion {
    return ReSharperVersion.fromString(s)
  }
}