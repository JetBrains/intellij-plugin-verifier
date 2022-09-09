package com.jetbrains.plugin.structure.domain

import com.jetbrains.plugin.structure.dotnet.version.ReSharperVersion
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
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
    IdeVersion.createIdeVersion("test.3.3")
  }

  @Test(expected = IllegalArgumentException::class)
  fun wrongBuildVersionTest() {
    IdeVersion.createIdeVersion("138.test.3")
  }

  @Test(expected = IllegalArgumentException::class)
  fun wrongMinorVersionTest() {
    IdeVersion.createIdeVersion("138.3.test")
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

  private fun version(s: String): ReSharperVersion {
    return ReSharperVersion.fromString(s)
  }
}