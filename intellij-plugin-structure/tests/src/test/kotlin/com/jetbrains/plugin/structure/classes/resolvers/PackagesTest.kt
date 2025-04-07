package com.jetbrains.plugin.structure.classes.resolvers

import org.junit.Assert.*
import org.junit.Test

class PackagesTest {
  @Test
  fun `packages in the package set are found, others are not`() {
    val packages = Packages()
    packages.addClass("com/example/foo/FooService")
    packages.addClass("com/example/bar/BarService")

    assertTrue(packages.contains("com/example"))
    assertTrue(packages.contains("com/example/bar"))

    assertTrue(packages.contains("com"))

    assertFalse(packages.contains("org/example"))
  }

  @Test
  fun `all packages, include recursive, are listed`() {
    val packages = Packages()
    packages.addClass("com/example/foo/FooService")
    packages.addClass("com/example/bar/BarService")

    with(packages.all) {
      assertEquals(4, size)
      assertEquals(setOf("com", "com/example", "com/example/bar", "com/example/foo"), this)
    }
  }

  @Test
  fun `empty package collection`() {
    val packages = Packages()
    with(packages.all) {
      assertEquals(0, size)
      assertTrue(isEmpty())
    }
  }
}