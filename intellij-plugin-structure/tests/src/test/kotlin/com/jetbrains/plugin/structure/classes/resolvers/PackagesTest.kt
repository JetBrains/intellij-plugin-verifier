/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.classes.resolvers

import com.jetbrains.plugin.structure.jar.Packages
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
  fun `all packages are listed`() {
    val packages = Packages()
    packages.addClass("com/example/foo/FooService")
    packages.addClass("com/example/bar/BarService")
    packages.addClass("com/example/bar/zap/ZapService")
    packages.addClass("com/jetbrains/foo/JBFooService")
    packages.addClass("com/jetbrains/cli/JBCliService")
    packages.addClass("com/jetbrains/cli/impl/JBCliServiceImpl")

    with(packages.entries) {
      assertEquals(6, size)
      assertEquals(
        setOf("com/example/foo",
          "com/example/bar",
          "com/example/bar/zap",
          "com/jetbrains/foo",
          "com/jetbrains/cli",
          "com/jetbrains/cli/impl"), this)
    }
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
  fun `test packages with strange names are listed correctly`() {
    val packages  = Packages()
    val classes = listOf(
      "com/test/$01_convertor/CreateSubSpringBootModule.class",
      "com/test/$01_convertor/CreateSubSpringBootModuleDialog\$CreateNewSpringBootModuleListener.class",
      "com/test/$01_convertor/CreateSubSpringBootModuleDialog.class",
      "com/test/$02_convert/ConvertMavenToSpringBoot.class",
      "com/test/$02_convert/ConvertMavenToSpringBootDialog\$ConvertMavenToSpringBootListener.class",
      "com/test/$02_convert/ConvertMavenToSpringBootDialog.class",
      "com/test/TestPackageClass.class",
      "com/test/utils/FileUtils.class",
      )
    classes.forEach { packages.addClass(it) }
    assertEquals(4, packages.entries.size)
  }

  @Test
  fun `empty package collection`() {
    val packages = Packages()
    with(packages.all) {
      assertEquals(0, size)
      assertTrue(isEmpty())
    }
  }

  @Test
  fun `packages are inserted and retrieved`() {
    val packages = Packages()
    packages.addPackage("com/example/foo")
    // duplicate insertion
    packages.addPackage("com/example/foo")
    packages.addPackage("com/example/bar")

    assertTrue(packages.contains("com/example"))
    assertTrue(packages.contains("com/example/bar"))

    assertTrue(packages.contains("com"))

    assertFalse(packages.contains("org/example"))

    assertEquals(setOf("com/example/foo", "com/example/bar"), packages.entries)
    assertEquals(setOf("com", "com/example", "com/example/foo", "com/example/bar"), packages.all)
  }

  @Test
  fun `class with empty package is inserted`() {
    val packages = Packages()
    packages.addClass("FooService")

    assertTrue(packages.contains(""))

    assertEquals(setOf(""), packages.entries)
    assertEquals(setOf(""), packages.all)
  }


}