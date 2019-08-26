package com.jetbrains.pluginverifier.files

import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import org.junit.Assert
import org.junit.Test

class SpaceAmountTest {
  @Test
  fun presentations() {
    Assert.assertEquals("0 B", SpaceAmount.ofBytes(0).presentableAmount())

    Assert.assertEquals("1 B", SpaceAmount.ofBytes(1).presentableAmount())
    Assert.assertEquals("1023 B", SpaceAmount.ofBytes(1023).presentableAmount())

    Assert.assertEquals("1 KB", SpaceAmount.ofKilobytes(1).presentableAmount())
    Assert.assertEquals("1.00 KB", SpaceAmount.ofBytes(1025).presentableAmount())
    Assert.assertEquals("1.50 KB", SpaceAmount.ofBytes(1024 + 512).presentableAmount())
    Assert.assertEquals("1023 KB", SpaceAmount.ofKilobytes(1023).presentableAmount())

    Assert.assertEquals("1 MB", SpaceAmount.ofMegabytes(1).presentableAmount())
    Assert.assertEquals("1.25 MB", SpaceAmount.ofKilobytes(1024 + 256).presentableAmount())
    Assert.assertEquals("1023 MB", SpaceAmount.ofMegabytes(1023).presentableAmount())
    Assert.assertEquals("1.00 GB", SpaceAmount.ofMegabytes(1025).presentableAmount())

    Assert.assertEquals("1 GB", SpaceAmount.ofGigabytes(1).presentableAmount())
  }
}