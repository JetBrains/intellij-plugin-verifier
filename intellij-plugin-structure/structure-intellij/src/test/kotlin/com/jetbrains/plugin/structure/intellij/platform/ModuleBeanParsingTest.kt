package com.jetbrains.plugin.structure.intellij.platform

import com.jetbrains.plugin.structure.intellij.beans.ModuleBean
import com.jetbrains.plugin.structure.intellij.extractor.ModuleUnmarshaller
import junit.framework.TestCase.assertNotNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.nio.file.Path
import javax.xml.bind.UnmarshalException

class ModuleBeanParsingTest {
  @Test
  fun `module XML is parsed`() {
    val xml = """
      <module name="intellij.notebooks.ui">
        <dependencies>
          <module name="intellij.platform.lang"/>
          <module name="intellij.platform.core.ui"/>
        </dependencies>
        <resources>
          <resource-root path="../lib/modules/intellij.notebooks.ui.jar"/>
        </resources>
      </module>
      """.trimIndent()
    val module = xml.unmarshall()
    assertNotNull(module)
    assertEquals("intellij.notebooks.ui", module.name)
    with(module) {
      assertEquals(2, dependencies.size)
      assertEquals("intellij.platform.lang", dependencies[0].name)
      assertEquals("intellij.platform.core.ui", dependencies[1].name)

      assertEquals(1, resources.size)
      assertEquals(Path.of("../lib/modules/intellij.notebooks.ui.jar"), resources[0].path)
    }
  }

  @Test
  fun `invalid module XML`() {
    val xml = """
      <random-element />
      """.trimIndent()
    assertThrows(UnmarshalException::class.java) {
      xml.unmarshall()
    }
  }

  private fun String.unmarshall(): ModuleBean {
    return ModuleUnmarshaller.unmarshall(this)
  }
}
