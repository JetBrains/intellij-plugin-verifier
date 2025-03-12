package com.jetbrains.plugin.structure.intellij.plugin.descriptors

import com.jetbrains.plugin.structure.base.utils.closeAll
import com.jetbrains.plugin.structure.base.utils.inputStream
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.nio.file.Path
import javax.xml.stream.XMLEventReader
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


  // FIXME duplicate with XmlStreamEventFilter
  private fun newXmlInputFactory() = XMLInputFactory.newInstance().apply {
    setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false)
    setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false)
  }

  // FIXME duplicate with XmlStreamings
  class CloseableXmlEventReader(private val delegate: XMLEventReader) : XMLEventReader by delegate, Closeable {
    @Throws(XMLStreamException::class)
    override fun close() {
      delegate.close()
    }
  }

  // FIXME duplicate with XmlStreamEventFilter
  private fun CloseableXmlEventReader.hasNextEvent(): Boolean {
    return try {
      hasNext()
    } catch (e: XMLStreamException) {
      LOG.error("Cannot retrieve next event", e)
      false
    } catch (e: RuntimeException) {
      LOG.error("Cannot retrieve next event", e)
      false
    }
  }
}