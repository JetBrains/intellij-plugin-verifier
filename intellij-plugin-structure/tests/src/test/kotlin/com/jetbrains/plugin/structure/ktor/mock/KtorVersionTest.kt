package com.jetbrains.plugin.structure.ktor.mock

import com.jetbrains.plugin.structure.base.utils.CompatibilityUtils
import com.jetbrains.plugin.structure.ktor.KtorFeature
import com.jetbrains.plugin.structure.ktor.KtorFeaturePluginManager
import com.jetbrains.plugin.structure.ktor.version.KtorVersion
import com.jetbrains.plugin.structure.mocks.BasePluginManagerTest
import com.jetbrains.plugin.structure.rules.FileSystemType
import org.hamcrest.core.IsNull
import org.junit.Assert
import org.junit.Test
import java.nio.file.Path


class KtorVersionTest(fileSystemType: FileSystemType) : BasePluginManagerTest<KtorFeature, KtorFeaturePluginManager>(fileSystemType) {
  override fun createManager(extractDirectory: Path): KtorFeaturePluginManager
    = KtorFeaturePluginManager.createManager(extractDirectory)

  @Test
  fun `correct ktor versions`() {
    KtorVersion.fromString("1.4.0")
    KtorVersion.fromString("1.4.0-Snapshot")
    KtorVersion.fromString("1.0.0")
    KtorVersion.fromString("999.999.999")
  }

  @Test
  fun `incorrect ktor versions`() {
    val incorrectVersions = listOf(
      "1",
      "1.4",
      "1..1",
      "..1",
      "1.4.*",
      "*",
      ""
    )

    for (v in incorrectVersions) {
      val result = try {
        KtorVersion.fromString(v)
      } catch (e: IllegalArgumentException) {
        null
      }

      Assert.assertThat("Ktor version $v is incorrect", result, IsNull.nullValue())
    }
  }

  @Test
  fun `ktor version as long`() {
    val data = mapOf<String, Long>(
      "1.4.0" to 1000040000,
      "1.4.1" to 1000040001,
      "1.5.0" to 1000050000,
      "2.4.0" to 2000040000,
      "1.99999.0" to 1999990000,
      "1.9999999.0" to 1999990000,
      "1111.9999999.0" to 1111999990000
    )

    data.forEach { (k, v) ->
      val version = KtorVersion.fromString(k)
      val asLong = CompatibilityUtils.versionAsLong(version.x, version.y, version.z)

      Assert.assertEquals("Ktor version (as long) is not as expected", v, asLong)
    }
  }

  @Test
  fun `snapshot is ignored in version`() {
    val withSnapshot = KtorVersion.fromString("1.4.0-Snapshot")
    val withoutSnapshot = KtorVersion.fromString("1.4.0")

    val withSnapshotAsLong = CompatibilityUtils.versionAsLong(withSnapshot.x, withSnapshot.y, withSnapshot.z)
    val withoutSnapshotAsLong = CompatibilityUtils.versionAsLong(withoutSnapshot.x, withoutSnapshot.y, withoutSnapshot.z)


    Assert.assertEquals(withSnapshot.asString(), withoutSnapshot.asString())
    Assert.assertEquals(withSnapshotAsLong, withoutSnapshotAsLong)
  }


}