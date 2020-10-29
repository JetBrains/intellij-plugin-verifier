package com.jetbrains.plugin.structure.edu.mock

import com.jetbrains.plugin.structure.edu.EduFullPluginVersion
import org.hamcrest.core.IsNull
import org.junit.Assert
import org.junit.Test


class EduVersionTest {
  @Test
  fun `correct edu versions`() {
    EduFullPluginVersion.fromString("3.7-2019.3-5266")
    EduFullPluginVersion.fromString("3.7.1-2019.3-5266")
    EduFullPluginVersion.fromString("1.0.0-2019.3-5266")
    EduFullPluginVersion.fromString("999.999.999-2019.3-5266")
  }

  @Test
  fun `incorrect edu versions`() {
    val incorrectVersions = listOf(
      "3-2019.3-5266",
      "-2019.3-5266",
      "3.7-2019.3-",
      "3.7-2019.3",
      "3.7--5266",
      "3.7",
      "1.4.*-2019.3-5266",
      "1.*.*-2019.3-5266",
      "*-2019.3-5266",
      ""
    )

    for (v in incorrectVersions) {
      val result = try {
        EduFullPluginVersion.fromString(v)
      } catch (e: IllegalArgumentException) {
        null
      }

      Assert.assertThat("Edu full plugin version $v is incorrect", result, IsNull.nullValue())
    }
  }

  @Test
  fun `edu version as long`() {
    val data = mapOf<String, Long>(
      "1.4.0-2019.3-5266" to 1000040000,
      "1.4-2019.3-5266" to 1000040000,
      "1.4.1-2019.3-5266" to 1000040001,
      "1.5-2019.3-5266" to 1000050000,
      "2.4-2019.3-5266" to 2000040000,
      "1.99999-2019.3-5266" to 1999990000,
      "1.9999999-2019.3-5266" to 1999990000,
      "1111.9999999-2019.3-5266" to 1111999990000
    )

    data.forEach { (k, v) ->
      val version = EduFullPluginVersion.fromString(k)

      Assert.assertEquals("Edu version (as long) is not as expected", v, version.asLong())
    }
  }
}