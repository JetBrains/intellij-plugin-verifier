package com.jetbrains.pluginverifier.tests.mocks

import com.google.common.jimfs.Configuration.unix
import com.google.common.jimfs.Jimfs
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables
import uk.org.webcompere.systemstubs.properties.SystemProperties
import uk.org.webcompere.systemstubs.resource.Resources
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.invariantSeparatorsPathString

private const val MOCK_PV_USER_HOME = "/home/pv"

class JdkDescriptorProviderTest {
  @Before
  fun setUp() {
    val fs = Jimfs.newFileSystem(unix())
    TestJdkDescriptorProvider.filesystem = fs
    Files.createDirectories(fs.getPath("/opt/java8"))
    Files.createDirectories(fs.getPath("/opt/java11"))
    Files.createDirectories(fs.getPath("$MOCK_PV_USER_HOME/.sdkman/candidates/java/current"))
  }

  @Test
  fun `discover JDK in JAVA_HOME`() {
    Resources.with<Unit>(EnvironmentVariables("JAVA_HOME", "/opt/java8"))
      .execute {
        val jdkPathForTests = TestJdkDescriptorProvider.getJdkPathForTests()
        assertEquals(Path("/opt/java8"), jdkPathForTests)
      }
  }

  @Test
  fun `discover JDK in test system property`() {
    Resources.with<Unit>(
      SystemProperties(PV_TESTJAVA_HOME_PROPERTY_NAME, "/opt/java11").set("user.home", "/nonexistent"))
      .execute {
        val jdkPathForTests = TestJdkDescriptorProvider.getJdkPathForTests()
        assertEquals(Path("/opt/java11"), jdkPathForTests)
      }
  }

  @Test
  fun `discover JDK in system property`() {
    Resources.with<Unit>(EnvironmentVariables("JAVA_HOME", null),
      SystemProperties("java.home", "/opt/java11").set("user.home", "/nonexistent"))
      .execute {
        val jdkPathForTests = TestJdkDescriptorProvider.getJdkPathForTests()
        assertEquals(Path("/opt/java11"), jdkPathForTests)
      }
  }

  @Test
  fun `discover JDK in SDKMan property`() {
    Resources.with<Unit>(EnvironmentVariables("JAVA_HOME", null),
      SystemProperties("user.home", MOCK_PV_USER_HOME))
      .execute {
        val jdkPathForTests = TestJdkDescriptorProvider.getJdkPathForTests()
        assertEquals(Path("$MOCK_PV_USER_HOME/.sdkman/candidates/java/current"), jdkPathForTests)
      }
  }

  private fun assertEquals(expected: Path, actual: Path) {
    assertEquals(expected.invariantSeparatorsPathString, actual.invariantSeparatorsPathString)
  }
}