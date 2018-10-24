package org.jetbrains.ide.diff.builder.persistence

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.checkEquals
import org.jetbrains.ide.diff.builder.signatures.ApiSignature
import org.jetbrains.ide.diff.builder.signatures.parseApiSignature
import org.jetbrains.ide.diff.builder.signatures.unescapeHtml
import java.io.Closeable
import java.io.Reader
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamReader

/**
 * Utility class used to read `annotations.xml` files corresponding to package [packageName].
 */
class SinceApiXmlReader(
    private val packageName: String,
    private val reader: Reader
) : Closeable {

  private companion object {
    val xmlInputFactory: XMLInputFactory by lazy {
      XMLInputFactory.newFactory()
    }
  }

  private val xmlInput = xmlInputFactory.createXMLStreamReader(reader)

  /**
   * Reads next [ApiSignature] and [IdeVersion] where this signature
   * was first introduced, from the configured stream.
   * Returns `null` if no more signatures left unread.
   */
  fun readNextSignature(): Pair<ApiSignature, IdeVersion>? {
    if (!xmlInput.hasNext()) {
      return null
    }

    var apiSignature: ApiSignature? = null
    var insideAnnotation = false

    while (xmlInput.hasNext()) {
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
            checkEquals(AVAILABLE_SINCE_ANNOTATION_NAME, xmlInput.getAttributeValue(0))
            insideAnnotation = true
          }
          "val" -> {
            checkNotNull(apiSignature) { "<val> before <item>" }
            check(insideAnnotation) { "<val> before <annotation>" }
            checkEquals("name", xmlInput.getAttributeLocalName(0))
            checkEquals("value", xmlInput.getAttributeValue(0))

            checkEquals("val", xmlInput.getAttributeLocalName(1))
            val value = xmlInput.getAttributeValue(1).unescapeHtml()

            check(value.startsWith('\"') && value.endsWith('\"')) { value }
            val sinceVersion = IdeVersion.createIdeVersion(value.trim('\"'))
            return apiSignature!! to sinceVersion
          }
        }
      }
      if (xmlInput.eventType == XMLStreamReader.END_ELEMENT) {
        when (xmlInput.localName) {
          "item" -> apiSignature = null
          "annotation" -> insideAnnotation = false
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