package com.jetbrains.plugin.structure.resolvers

import com.jetbrains.plugin.structure.classes.resolvers.ClassFilesResolver
import com.jetbrains.plugin.structure.classes.resolvers.InvalidClassFileException
import com.jetbrains.plugin.structure.classes.resolvers.JarFileResolver
import com.jetbrains.plugin.structure.classes.resolvers.ResolutionResult
import com.jetbrains.plugin.structure.testUtils.contentBuilder.buildDirectory
import com.jetbrains.plugin.structure.testUtils.contentBuilder.buildZipFile
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Tests that [InvalidClassFileException] is thrown on attempts to read invalid class files.
 */
class InvalidClassFileTest {

  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  @Test
  fun `read invalid class file from local class file fails in constructor`() {
    val classFilesRoot = buildDirectory(temporaryFolder.newFolder()) {
      file("invalid.class", "bad")
    }
    try {
      ClassFilesResolver(classFilesRoot).use { }
    } catch (e: InvalidClassFileException) {
      assertEquals(
          "Unable to read class 'invalid' using the ASM Java Bytecode engineering library. The internal ASM error: java.lang.ArrayIndexOutOfBoundsException: 6.",
          e.message
      )
      return
    }
    fail()
  }

  @Test
  fun `read invalid class file from jar`() {
    val jarFile = buildZipFile(temporaryFolder.newFile("invalid.jar")) {
      file("invalid.class", "bad")
    }

    JarFileResolver(jarFile.toPath()).use { jarResolver ->
      val invalidResult = jarResolver.resolveClass("invalid") as ResolutionResult.InvalidClassFile
      assertEquals(
          "Unable to read class 'invalid' using the ASM Java Bytecode engineering library. The internal ASM error: java.lang.ArrayIndexOutOfBoundsException: 6.",
          invalidResult.message
      )
    }
  }
}