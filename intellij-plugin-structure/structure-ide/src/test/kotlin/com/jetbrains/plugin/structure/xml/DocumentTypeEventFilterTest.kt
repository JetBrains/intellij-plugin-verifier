package com.jetbrains.plugin.structure.xml

import org.intellij.lang.annotations.Language
import org.junit.Assert.assertEquals
import org.junit.Test
import javax.xml.stream.EventFilter
import javax.xml.stream.XMLStreamConstants.START_DOCUMENT
import javax.xml.stream.events.XMLEvent

class DocumentTypeEventFilterTest : BaseEventFilterTest() {
  @Test
  fun `XML with pass-thru document-type is written as-is`() {
    @Language("XML") val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <module name="intellij.platform.coverage">
        <dependencies>
          <module name="intellij.platform.ide"/>
        </dependencies>
        <resources>
          <resource-root path="../lib/modules/intellij.platform.coverage.jar"/>
        </resources>
      </module>
    """.trimIndent()

    @Language("XML") val expectedXml = """
    <?xml version='1.0' encoding='UTF-8'?><module name="intellij.platform.coverage">
      <dependencies>
        <module name="intellij.platform.ide"/>
      </dependencies>
      <resources>
        <resource-root path="../lib/modules/intellij.platform.coverage.jar"/>
      </resources>
    </module>
    """.trimIndent()


    val eventLog = EventLogFilter()
    val xmlStreamEventFilter = XmlStreamEventFilter()
    val transformationContext = XmlTransformationContext.create()

    val documentTypeFilter = DocumentTypeFilter(setOf("idea-project"), eventLog)
    val filteredXml = captureToString {
      xmlStreamEventFilter.filter(documentTypeFilter, xml.toInputStream(), this, transformationContext)
    }

    assertEquals(expectedXml, filteredXml)
  }

  @Test
  fun `XML with unrecognized document-type is passed to a delegate filter`() {
    @Language("XML") val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <idea-plugin>
      </idea-plugin>
    """.trimIndent()

    val eventLog = EventLogFilter(isDeduplicating = true)
    val xmlStreamEventFilter = XmlStreamEventFilter()
    val transformationContext = XmlTransformationContext.create()

    val documentTypeFilter = DocumentTypeFilter(setOf("module"), eventLog)
    val actualXml = captureToString {
      xmlStreamEventFilter.filter(documentTypeFilter, xml.toInputStream(), this, transformationContext)
    }

    val expectedEventTypes = listOf(START_DOCUMENT)
    assertEquals(expectedEventTypes.size, eventLog.events.size)

    val expectedXml = """
      <?xml version='1.0' encoding='UTF-8'?><idea-plugin>
      </idea-plugin>
    """.trimIndent()
    assertEquals(expectedXml, actualXml)
  }

  @Test
  fun `XML with supported element that is not a root is handled`() {
    val eventLog = EventLogFilter(isDeduplicating = true)
    val xmlStreamEventFilter = XmlStreamEventFilter()
    val transformationContext = XmlTransformationContext.create()

    val documentTypeFilter = DocumentTypeFilter(setOf("module"), eventLog)
    val filteredXml = captureToString {
      xmlStreamEventFilter.filter(documentTypeFilter, projectXml.toInputStream(), this, transformationContext)
    }

    @Language("XML")
    val expectedXml = """
      <?xml version='1.0' encoding='UTF-8'?><project version="4">
        <component name="ProjectModuleManager">
          <modules>
            <module fileurl="file://PROJECT_DIR/.idea/qodana-embedded-profiles.iml" filepath="PROJECT_DIR/.idea/qodana-embedded-profiles.iml"/>
          </modules>
        </component>
      </project>
    """.trimIndent()

    assertEquals(expectedXml, filteredXml)
  }

  val projectXml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <project version="4">
        <component name="ProjectModuleManager">
          <modules>
            <module fileurl="file://PROJECT_DIR/.idea/qodana-embedded-profiles.iml" filepath="PROJECT_DIR/.idea/qodana-embedded-profiles.iml" />
          </modules>
        </component>
      </project>    
  """.trimIndent()

  private class EventLogFilter(private val isDeduplicating: Boolean = false) : EventFilter {
    val events = mutableListOf<XMLEvent>()

    override fun accept(event: XMLEvent): Boolean {
      if (events.lastOrNull() === event && isDeduplicating) {
        return true
      }
      events += event
      return true
    }
  }
}