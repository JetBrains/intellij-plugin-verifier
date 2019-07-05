package com.jetbrains.plugin.structure.resolvers

import com.jetbrains.plugin.structure.classes.resolvers.ClassFilesResolver
import com.jetbrains.plugin.structure.classes.resolvers.InvalidClassFileException
import com.jetbrains.plugin.structure.classes.resolvers.JarFileResolver
import com.jetbrains.plugin.structure.testUtils.contentBuilder.buildDirectory
import com.jetbrains.plugin.structure.testUtils.contentBuilder.buildZipFile
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.rules.TemporaryFolder

/**
 * Tests that [InvalidClassFileException] is thrown on attempts to read invalid class files.
 */
class InvalidClassFileTest {

  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  @Rule
  @JvmField
  val expectedEx: ExpectedException = ExpectedException.none()

  @Test
  fun `read invalid class file from local class file`() {
    expectedEx.expect(InvalidClassFileException::class.java)
    expectedEx.expectMessage("Unable to read class-file `invalid.class` using the ASM Java Bytecode engineering library. The internal ASM error: java.lang.ArrayIndexOutOfBoundsException: 6.")

    val classFilesRoot = buildDirectory(temporaryFolder.newFolder()) {
      file("invalid.class", "bad")
    }
    ClassFilesResolver(classFilesRoot).use { resolver ->
      resolver.findClass("invalid")
    }
  }

  @Test
  fun `read invalid class file from jar`() {
    expectedEx.expect(InvalidClassFileException::class.java)
    expectedEx.expectMessage("Unable to read class-file `invalid` using the ASM Java Bytecode engineering library. The internal ASM error: java.lang.ArrayIndexOutOfBoundsException: 6.")

    val jarFile = buildZipFile(temporaryFolder.newFile("invalid.jar")) {
      file("invalid.class", "bad")
    }

    JarFileResolver(jarFile.toPath()).use { jarResolver ->
      jarResolver.findClass("invalid")
    }
  }
}