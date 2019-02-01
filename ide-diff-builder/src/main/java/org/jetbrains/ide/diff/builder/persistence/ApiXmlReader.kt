package org.jetbrains.ide.diff.builder.persistence

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.checkEquals
import org.jetbrains.ide.diff.builder.api.ApiEvent
import org.jetbrains.ide.diff.builder.api.IntroducedIn
import org.jetbrains.ide.diff.builder.api.RemovedIn
import org.jetbrains.ide.diff.builder.signatures.ApiSignature
import org.jetbrains.ide.diff.builder.signatures.parseApiSignature
import org.jetbrains.ide.diff.builder.signatures.unescapeHtml
import java.io.Closeable
import java.io.Reader
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamReader

/**
 * Utility class used to read `annotations.xml` corresponding to package [packageName].
 */
class ApiXmlReader(private val packageName: String, private val reader: Reader) : Closeable {

  private companion object {
    val xmlInputFactory: XMLInputFactory by lazy {
      XMLInputFactory.newFactory()
    }
  }

  private val xmlInput = xmlInputFactory.createXMLStreamReader(reader)

  /**
   * Reads next [ApiSignature] and corresponding [ApiEvent].
   * Returns `null` if no more signatures left unread.
   */
  fun readNextSignature(): Pair<ApiSignature, ApiEvent>? {
    if (!xmlInput.hasNext()) {
      return null
    }

    var apiSignature: ApiSignature? = null
    var apiEventAnnotation: ApiEventAnnotation? = null

    whileLoop@ while (xmlInput.hasNext()) {
      xmlInput.next()
      if (xmlInput.eventType == XMLStreamReader.START_ELEMENT) {
        when (xmlInput.localName) {
          "item" -> {
            checkEquals("name", xmlInput.getAttributeLocalName(0))
            val itemName = xmlInput.getAttributeValue(0).unescapeHtml()
            apiSignature = parseApiSignature(packageName, itemName)
          }
          "annotation" -> {
            checkEquals("name", xmlInput.getAttributeLocalName(0))
            apiEventAnnotation = when (xmlInput.getAttributeValue(0)) {
              AvailableSinceAnnotation.annotationName -> AvailableSinceAnnotation
              ScheduledForRemovalAnnotation.annotationName -> ScheduledForRemovalAnnotation
              else -> continue@whileLoop
            }
          }
          "val" -> {
            checkNotNull(apiSignature) { "<val> before <item>" }
            checkNotNull(apiEventAnnotation) { "<val> before <annotation>" }
            checkEquals("name", xmlInput.getAttributeLocalName(0))
            checkEquals(apiEventAnnotation!!.valueName, xmlInput.getAttributeValue(0))

            checkEquals("val", xmlInput.getAttributeLocalName(1))
            val value = xmlInput.getAttributeValue(1).unescapeHtml()

            check(value.startsWith('\"') && value.endsWith('\"')) { value }
            val clearValue = value.trim('\"')
            val apiEvent = when (apiEventAnnotation) {
              AvailableSinceAnnotation -> IntroducedIn(IdeVersion.createIdeVersion(clearValue))
              ScheduledForRemovalAnnotation -> RemovedIn(IdeVersion.createIdeVersion(clearValue))
            }
            return apiSignature!! to apiEvent
          }
        }
      }
      if (xmlInput.eventType == XMLStreamReader.END_ELEMENT) {
        when (xmlInput.localName) {
          "item" -> apiSignature = null
          "annotation" -> apiEventAnnotation = null
        }
      }
    }
    return null
  }

  override fun close() {
    /**
     * Close both xmlInput and its reader, since [XMLStreamReader.close] doesn't close the associated reader.
     */
    reader.use {
      xmlInput.close()
    }
  }
}