/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.fleet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FleetShipVersionRangeTest {
  @Test
  fun shouldParseVersionRange() {
    assertEquals("should default to legacy version", 135004260, FleetShipVersionRange.fromStringToLong("1.48.100"))
    assertEquals("should parse product with legacy versioning", 135004260, FleetShipVersionRange.fromStringToLong("1.48.100", setOf("FL")))
    assertEquals("should parse product with unified versioning", 251123451234, FleetShipVersionRange.fromStringToLong("251.12345.1234", supportedProducts = setOf("AIR")))
    assertEquals("should parse product with nightly unified versioning", 251123450000, FleetShipVersionRange.fromStringToLong("251.12345", supportedProducts = setOf("AIR")))
    assertThrows("should not support mix and match of unified and legacy versioning", IllegalArgumentException::class.java) { FleetShipVersionRange.fromStringToLong("1.48.100", setOf("FL", "AIR")) }
    assertThrows("should not support mix and match of unified and legacy versioning", IllegalArgumentException::class.java) { FleetShipVersionRange.fromStringToLong("251.12345.1234", setOf("FL", "AIR")) }
    assertThrows("should not support non-existing Fleet products", IllegalArgumentException::class.java) { FleetShipVersionRange.fromStringToLong("251.12345.1234", setOf("NOTEXISTING")) }
  }
}