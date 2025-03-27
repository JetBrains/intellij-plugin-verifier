package com.jetbrains.plugin.structure.intellij.platform

import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.jar.JarFileSystemProvider
import com.jetbrains.plugin.structure.jar.SingletonCachingJarFileSystemProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.file.Path

private const val MODULES = "modules"
private const val MOCK_IDE = "mock-ide"
private const val MODULE_DESCRIPTORS_JAR= "module-descriptors.jar"

class BundledModulesResolverTest {

  @JvmField
  @Rule
  var tempFolder = TemporaryFolder()

  private lateinit var mockIde: Path
  private lateinit var moduleDescriptorsJar: Path
  private lateinit var fileSystemProvider: JarFileSystemProvider

  @Before
  fun setUp() {
    val mockIdeDir: File = tempFolder.newFolder(MOCK_IDE)
    tempFolder.newFolder("$MOCK_IDE/$MODULES")
    mockIde = mockIdeDir.toPath()

    moduleDescriptorsJar = tempFolder.newFile("$MOCK_IDE/$MODULES/$MODULE_DESCRIPTORS_JAR").toPath()
    fileSystemProvider = SingletonCachingJarFileSystemProvider
  }

  @Test
  fun `modules are resolved`() {
    buildZipFile(moduleDescriptorsJar) {
      file("moduleOne.xml") {
        """
        <module name="moduleOne">
          <dependencies>
            <module name="intellij.platform.lang"/>
          </dependencies>
          <resources>
            <resource-root path="../lib/modules/moduleOne.jar"/>
          </resources>
        </module>
        """.trimIndent()
      }
      file("moduleTwo.xml") {
        """
        <module name="moduleTwo">
          <dependencies>
            <module name="intellij.platform.lang"/>
          </dependencies>
          <resources>
            <resource-root path="../lib/modules/moduleTwo.jar"/>
          </resources>
        </module>
        """.trimIndent()
      }
    }

    val bundledModulesResolver = BundledModulesResolver(mockIde, fileSystemProvider)
    val modules = bundledModulesResolver.resolveModules()
    assertEquals(2, modules.size)
  }

  @Test
  fun `one legal and one illegal modules are resolved`() {
    buildZipFile(moduleDescriptorsJar) {
      file("moduleOne.xml") {
        """
        <module name="moduleOne">
          <dependencies>
            <module name="intellij.platform.lang"/>
          </dependencies>
          <resources>
            <resource-root path="../lib/modules/moduleOne.jar"/>
          </resources>
        </module>
        """.trimIndent()
      }
      file("illegal.xml") {
        """
        <random-element />
        """.trimIndent()
      }
    }

    val bundledModulesResolver = BundledModulesResolver(mockIde, fileSystemProvider)
    val modules = bundledModulesResolver.resolveModules()
    assertEquals(1, modules.size)
  }

}