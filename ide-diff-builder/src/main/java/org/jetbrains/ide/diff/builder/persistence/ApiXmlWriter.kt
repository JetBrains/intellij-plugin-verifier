package org.jetbrains.ide.diff.builder.persistence

import org.jetbrains.ide.diff.builder.api.ApiEvent
import org.jetbrains.ide.diff.builder.api.IntroducedIn
import org.jetbrains.ide.diff.builder.api.RemovedIn
import org.jetbrains.ide.diff.builder.signatures.ApiSignature
import org.jetbrains.ide.diff.builder.signatures.escapeHtml
import java.io.Closeable
import java.io.Writer

sealed class ApiEventAnnotation(val annotationName: String, val valueName: String)
object AvailableSinceAnnotation : ApiEventAnnotation("org.jetbrains.annotations.ApiStatus.AvailableSince", "value")
object ScheduledForRemovalAnnotation : ApiEventAnnotation("org.jetbrains.annotations.ApiStatus.ScheduledForRemoval", "inVersion")

const val ANNOTATIONS_XML_FILE_NAME = "annotations.xml"

const val BUILD_TXT_FILE_NAME = "build.txt"

/**
 * Utility class used to save API signatures belonging to one package to `annotations.xml`.
 */
internal class ApiXmlWriter(private val writer: Writer) : Closeable {

  fun appendXmlStart() {
    writer.appendln("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
    writer.appendln("<root>")
  }

  fun appendSignature(apiSignature: ApiSignature, apiEvent: ApiEvent) {
    val apiEventAnnotation = when (apiEvent) {
      is IntroducedIn -> AvailableSinceAnnotation
      is RemovedIn -> ScheduledForRemovalAnnotation
    }

    val ideVersion = when (apiEvent) {
      is IntroducedIn -> apiEvent.ideVersion
      is RemovedIn -> apiEvent.ideVersion
    }

    with(writer) {
      appendln("""  <item name="${apiSignature.externalPresentation.escapeHtml()}">""")
      appendln("""    <annotation name="${apiEventAnnotation.annotationName}">""")
      appendln("""      <val name="${apiEventAnnotation.valueName}" val="&quot;${ideVersion.asStringWithoutProductCode()}&quot;"/>""")
      appendln("""    </annotation>""")
      appendln("""  </item>""")
    }
  }

  fun appendXmlEnd() {
    writer.appendln("</root>")
  }

  override fun close() {
    writer.close()
  }
}

