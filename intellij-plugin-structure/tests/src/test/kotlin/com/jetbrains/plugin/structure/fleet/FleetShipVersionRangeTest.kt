/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.fleet

import org.junit.Assert.assertEquals
import org.junit.Test

class FleetShipVersionRangeTest {
  @Test
  fun shouldParseVersionRange() {
    assertEquals("should default to legacy version", 135004260, FleetShipVersionRange.fromStringToLong("1.48.100"))
    assertEquals("should default to legacy version for unknown", 135004260, FleetShipVersionRange.fromStringToLong("1.48.100", setOf("LOL")))
    assertEquals("should parse product with legacy versioning", 135004260, FleetShipVersionRange.fromStringToLong("1.48.100", setOf("FL")))
    assertEquals("should parse product with unified versioning", 251123451234, FleetShipVersionRange.fromStringToLong("251.12345.1234", supportedProducts = setOf("AIR")))
    assertEquals("should parse product with nightly unified versioning", 251123450000, FleetShipVersionRange.fromStringToLong("251.12345", supportedProducts = setOf("AIR")))
  }
}