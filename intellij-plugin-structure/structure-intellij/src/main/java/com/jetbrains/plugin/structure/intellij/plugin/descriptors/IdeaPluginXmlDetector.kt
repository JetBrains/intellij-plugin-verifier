package com.jetbrains.plugin.structure.intellij.plugin.descriptors

import com.jetbrains.plugin.structure.base.utils.closeAll
import com.jetbrains.plugin.structure.base.utils.inputStream
import com.jetbrains.plugin.structure.xml.CloseableXmlEventReader
import com.jetbrains.plugin.structure.xml.newXmlInputFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.nio.file.Path
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamException
import javax.xml.stream.events.StartElement
import javax.xml.stream.events.XMLEvent

private val LOG: Logger = LoggerFactory.getLogger(IdeaPluginXmlDetector::class.java)

private const val IDEA_PLUGIN_ROOT_ELEMENT = "idea-plugin"

class IdeaPluginXmlDetector {
  fun isPluginDescriptor(descriptorPath: Path): Boolean {
    val closeables = mutableListOf<Closeable>()
    try {
      val inputFactory: XMLInputFactory = newXmlInputFactory()
      val eventReader = inputFactory.newEventReader(descriptorPath).also { closeables += it }

      while (eventReader.hasNextEvent()) {
        val event: XMLEvent = eventReader.nextEvent()
        if (event.isIdeaPluginElement()) {
          return true
        }
      }
      return false
    } catch (e: XMLStreamException) {
      if (e.message?.contains("is reserved by the xml specification") == true) {
        LOG.debug("Unable to read plugin descriptor '{}': {}", descriptorPath, e.message)
        return false;
      } else {
        LOG.warn("Unable to read plugin descriptor '$descriptorPath'", e)
        return false
      }
    } catch (e: Exception) {
      LOG.warn("Unable to read plugin descriptor '$descriptorPath'", e)
      return false
    } finally {
      closeables.closeAll()
    }
  }

  private fun XMLEvent.isIdeaPluginElement(): Boolean =
    this is StartElement && name.localPart == IDEA_PLUGIN_ROOT_ELEMENT

  private fun XMLInputFactory.newEventReader(descriptorPath: Path): CloseableXmlEventReader {
    return CloseableXmlEventReader(createXMLEventReader(descriptorPath.inputStream()))
  }
}