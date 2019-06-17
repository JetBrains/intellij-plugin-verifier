package com.jetbrains.plugin.structure.resolvers

import com.jetbrains.plugin.structure.classes.resolvers.ClassFilesResolver
import com.jetbrains.plugin.structure.classes.resolvers.InvalidClassFileException
import com.jetbrains.plugin.structure.classes.resolvers.JarFileResolver
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

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

    val root = temporaryFolder.newFolder()
    val classFile = root.resolve("invalid.class")
    classFile.createNewFile()
    classFile.writeText("bad")
    ClassFilesResolver(root).use { resolver ->
      resolver.findClass("invalid")
    }
  }

  @Test
  fun `read invalid class file from jar`() {
    expectedEx.expect(InvalidClassFileException::class.java)
    expectedEx.expectMessage("Unable to read class-file `invalid` using the ASM Java Bytecode engineering library. The internal ASM error: java.lang.ArrayIndexOutOfBoundsException: 6.")

    val jarFile = temporaryFolder.newFile("invalid.jar").toPath()

    val jarOutputStream = JarOutputStream(Files.newOutputStream(jarFile))
    jarOutputStream.use {
      val jarEntry = JarEntry("invalid.class")
      jarOutputStream.putNextEntry(jarEntry)
      jarOutputStream.write("bad".toByteArray())
      jarOutputStream.closeEntry()
    }

    JarFileResolver(jarFile).use { jarResolver ->
      jarResolver.findClass("invalid")
    }
  }
}