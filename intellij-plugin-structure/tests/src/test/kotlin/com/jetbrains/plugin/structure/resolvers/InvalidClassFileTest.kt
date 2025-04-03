package com.jetbrains.plugin.structure.resolvers

import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildDirectory
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.classes.resolvers.DirectoryResolver
import com.jetbrains.plugin.structure.classes.resolvers.FileOrigin
import com.jetbrains.plugin.structure.classes.resolvers.InvalidClassFileException
import com.jetbrains.plugin.structure.classes.resolvers.LazyJarResolver
import com.jetbrains.plugin.structure.classes.resolvers.ResolutionResult
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import org.junit.Assert.assertTrue
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

  private object InvalidFileOrigin : FileOrigin {
    override val parent: FileOrigin? = null
  }

  @Test
  fun `read invalid class file from local class file fails in constructor`() {
    val classFilesRoot = buildDirectory(temporaryFolder.newFolder().toPath()) {
      file("invalid.class", "bad")
    }
    try {
      DirectoryResolver(classFilesRoot, InvalidFileOrigin).use { }
    } catch (e: InvalidClassFileException) {
      assertTrue(e.message.startsWith("Unable to read class 'invalid' using the ASM Java Bytecode engineering library. The internal ASM error: java.lang.ArrayIndexOutOfBoundsException"))
      return
    }
    fail()
  }

  @Test
  fun `read invalid class file from jar`() {
    val jarFile = buildZipFile(temporaryFolder.newFile("invalid.jar").toPath()) {
      file("invalid.class", "bad")
    }

    LazyJarResolver(jarFile, Resolver.ReadMode.FULL, InvalidFileOrigin).use { jarResolver ->
      val invalidResult = jarResolver.resolveClass("invalid") as ResolutionResult.Invalid
      assertTrue(invalidResult.message.startsWith("Unable to read class 'invalid' using the ASM Java Bytecode engineering library. The internal ASM error: java.lang.ArrayIndexOutOfBoundsException"))
    }
  }
}